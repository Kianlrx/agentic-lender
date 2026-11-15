// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

// LoanNote is the on-chain record of every approved mortgage.
// It does no calculations: the AI engine works out the APR and the green discount,
// the underwriter approves, and this contract checks the record and saves it for good.
contract LoanNote {
    // Loan is one approved mortgage, as recorded on the ledger.
    struct Loan {
        string applicationId; // the firestore document id of the application
        string borrowerId; // firebase uid of the applicant
        string underwriterId; // firebase uid of the staff member who approved it
        string bank; // the lender
        string propertyId; // the eircode, stored without spaces and in capitals
        uint256 loanAmount; // in whole euro
        uint256 termMonths; // how long the loan runs, in months
        uint256 apr; // in basis points, 475 = 4.75%. decided by the AI engine
        string berRating; // kept as a record only, the discount is already in the apr
        bytes32 auditHash; // fingerprint of the AI assessment, proves it was not edited later
        uint256 timestamp; // when it was recorded
        bool isActive; // true until the loan is closed (paid off or the house is sold)
    }

    // limits that any real mortgage has to be inside
    uint256 public constant MAX_APR = 1500; //15%, anything higher is a typo or fraud
    uint256 public constant MAX_TERM_MONTHS = 480; //40 years, same as the web form

    // owner is the wallet that deployed the contract (the bank). only it can record loans.
    address public owner;

    mapping(uint256 => Loan) public loans;
    uint256 public loanCount;

    // anti-fraud: which loan is active on each property (0 = none)
    mapping(bytes32 => uint256) public activeLoanOnProperty;
    // anti-fraud: which loan each application became (0 = not minted yet)
    mapping(bytes32 => uint256) public loanForApplication;

    event LoanMinted(uint256 loanId, string applicationId, string propertyId, uint256 amount, uint256 apr, bytes32 auditHash);
    event LoanClosed(uint256 loanId, string propertyId);

    // the wallet that deploys the contract becomes the owner.
    constructor() {
        owner = msg.sender;
    }

    // onlyOwner stops any other wallet from calling the function it is attached to.
    modifier onlyOwner() {
        require(msg.sender == owner, "Only the owner can mint loans");
        _; //runs the rest of the function
    }

    // mintLoan checks an approved mortgage and saves it as a new loan note.
    function mintLoan(
        string memory _applicationId,
        string memory _borrowerId,
        string memory _underwriterId,
        string memory _bank,
        string memory _propertyId,
        uint256 _loanAmount,
        uint256 _termMonths,
        uint256 _apr,
        string memory _berRating,
        bytes32 _auditHash
    ) public onlyOwner returns (uint256) {
        // every required value must be there
        require(bytes(_applicationId).length > 0, "Application ID is required");
        require(bytes(_borrowerId).length > 0, "Borrower ID is required");
        require(bytes(_underwriterId).length > 0, "Underwriter ID is required");
        require(bytes(_bank).length > 0, "Bank is required");
        require(_auditHash != bytes32(0), "Audit hash is required");

        // the numbers must make sense
        require(_loanAmount > 0, "Loan amount must be more than 0");
        require(_termMonths > 0 && _termMonths <= MAX_TERM_MONTHS, "Term must be 1 to 480 months");
        require(_apr > 0 && _apr <= MAX_APR, "APR must be between 0.01% and 15%");

        // anti-fraud: one approval can only be minted once
        bytes32 applicationKey = keccak256(bytes(_applicationId));
        require(loanForApplication[applicationKey] == 0, "Application already minted");

        // anti-fraud: only one active mortgage per property
        string memory cleanPropertyId = normaliseEircode(_propertyId);
        require(bytes(cleanPropertyId).length > 0, "Property ID is required");
        bytes32 propertyKey = keccak256(bytes(cleanPropertyId));
        require(activeLoanOnProperty[propertyKey] == 0, "Property already has an active loan");

        loanCount++; //loan ids start at 1
        loans[loanCount] = Loan({
            applicationId: _applicationId,
            borrowerId: _borrowerId,
            underwriterId: _underwriterId,
            bank: _bank,
            propertyId: cleanPropertyId,
            loanAmount: _loanAmount,
            termMonths: _termMonths,
            apr: _apr,
            berRating: _berRating,
            auditHash: _auditHash,
            timestamp: block.timestamp,
            isActive: true
        });
        activeLoanOnProperty[propertyKey] = loanCount;
        loanForApplication[applicationKey] = loanCount;

        emit LoanMinted(loanCount, _applicationId, cleanPropertyId, _loanAmount, _apr, _auditHash);
        return loanCount;
    }

    // closeLoan marks a loan as finished so the property can get a new mortgage.
    // the record itself stays on the ledger forever.
    function closeLoan(uint256 _loanId) public onlyOwner {
        Loan storage loan = loans[_loanId];
        require(loan.isActive, "Loan is not active");

        loan.isActive = false;
        activeLoanOnProperty[keccak256(bytes(loan.propertyId))] = 0;
        emit LoanClosed(_loanId, loan.propertyId);
    }

    // verifyAudit checks an AI assessment against the one recorded at minting.
    // true = untouched, false = the assessment was changed after approval.
    function verifyAudit(uint256 _loanId, bytes32 _hash) public view returns (bool) {
        return loans[_loanId].auditHash == _hash;
    }

    // getLoan lets anyone look up a loan note by its id.
    function getLoan(uint256 _loanId) public view returns (Loan memory) {
        return loans[_loanId];
    }

    // normaliseEircode removes spaces and makes letters capitals,
    // so "d02 x285" and "D02X285" count as the same house.
    function normaliseEircode(string memory _eircode) public pure returns (string memory) {
        bytes memory input = bytes(_eircode);
        bytes memory output = new bytes(input.length);
        uint256 length = 0;

        for (uint256 i = 0; i < input.length; i++) {
            bytes1 character = input[i];
            if (character == " ") {
                continue; //skip spaces
            }
            if (character >= "a" && character <= "z") {
                character = bytes1(uint8(character) - 32); //lowercase to capital
            }
            output[length] = character;
            length++;
        }

        // cut the unused space off the end
        bytes memory trimmed = new bytes(length);
        for (uint256 i = 0; i < length; i++) {
            trimmed[i] = output[i];
        }
        return string(trimmed);
    }
}
