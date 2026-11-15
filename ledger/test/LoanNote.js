const { expect } = require("chai");
const { ethers } = require("hardhat");

// tests for the LoanNote ledger: required fields, limits and the anti-fraud rules.
describe("LoanNote", function () {
  let loanNote; //the deployed contract
  let owner; //the bank, it deploys the contract
  let stranger; //any other wallet
  const auditHash = ethers.sha256(ethers.toUtf8Bytes("AI assessment")); //fake fingerprint of the AI assessment

  // approved builds a valid approved mortgage. pass in only the parts a test wants to change.
  function approved(changes = {}) {
    const loan = {
      applicationId: "app-001",
      borrowerId: "uid-borrower-1",
      underwriterId: "uid-underwriter-1",
      bank: "Agentic Lender",
      propertyId: "D02 X285",
      loanAmount: 250000,
      termMonths: 360,
      apr: 375, //3.75%, sent by the AI engine
      berRating: "B2",
      auditHash: auditHash,
      ...changes,
    };
    return [
      loan.applicationId, loan.borrowerId, loan.underwriterId, loan.bank, loan.propertyId,
      loan.loanAmount, loan.termMonths, loan.apr, loan.berRating, loan.auditHash,
    ];
  }

  // runs before every test so each one starts with a fresh contract.
  beforeEach(async function () {
    [owner, stranger] = await ethers.getSigners();
    const LoanNote = await ethers.getContractFactory("LoanNote");
    loanNote = await LoanNote.deploy();
  });

  // the wallet that deploys the contract must become the owner.
  it("sets the deployer as the owner", async function () {
    expect(await loanNote.owner()).to.equal(owner.address);
  });

  // a complete approved mortgage must be saved exactly as it was sent in.
  it("stores every field of an approved mortgage", async function () {
    await loanNote.mintLoan(...approved());
    const loan = await loanNote.getLoan(1);
    expect(loan.applicationId).to.equal("app-001");
    expect(loan.borrowerId).to.equal("uid-borrower-1");
    expect(loan.underwriterId).to.equal("uid-underwriter-1");
    expect(loan.bank).to.equal("Agentic Lender");
    expect(loan.propertyId).to.equal("D02X285"); //saved without the space
    expect(loan.loanAmount).to.equal(250000);
    expect(loan.termMonths).to.equal(360);
    expect(loan.berRating).to.equal("B2");
    expect(loan.auditHash).to.equal(auditHash);
    expect(loan.isActive).to.equal(true);
  });

  // the ledger does no maths: the APR saved must be the one the AI engine sent.
  it("stores the APR it is given without changing it", async function () {
    await loanNote.mintLoan(...approved({ apr: 400, berRating: "A1" }));
    expect((await loanNote.getLoan(1)).apr).to.equal(400); //no discount taken off
  });

  // loan ids start at 1 and go up by one for each new loan.
  it("numbers the loans 1, 2, 3", async function () {
    await loanNote.mintLoan(...approved());
    await loanNote.mintLoan(...approved({ applicationId: "app-002", propertyId: "T12 AB34" }));
    expect(await loanNote.loanCount()).to.equal(2);
    expect((await loanNote.getLoan(2)).applicationId).to.equal("app-002");
  });

  // minting must announce itself with the LoanMinted event.
  it("emits the LoanMinted event", async function () {
    await expect(loanNote.mintLoan(...approved()))
      .to.emit(loanNote, "LoanMinted")
      .withArgs(1, "app-001", "D02X285", 250000, 375, auditHash);
  });

  // only the owner may mint, any other wallet must be rejected.
  it("blocks anyone except the owner from minting", async function () {
    await expect(loanNote.connect(stranger).mintLoan(...approved()))
      .to.be.revertedWith("Only the owner can mint loans");
  });

  // every required text field must be filled in.
  it("rejects a mortgage with a missing ID or bank", async function () {
    const missing = [
      ["applicationId", "Application ID is required"],
      ["borrowerId", "Borrower ID is required"],
      ["underwriterId", "Underwriter ID is required"],
      ["bank", "Bank is required"],
      ["propertyId", "Property ID is required"],
    ];
    for (const [field, message] of missing) {
      await expect(loanNote.mintLoan(...approved({ [field]: "" })), field)
        .to.be.revertedWith(message);
    }
  });

  // a property ID made of only spaces is still empty.
  it("rejects a property ID that is only spaces", async function () {
    await expect(loanNote.mintLoan(...approved({ propertyId: "   " })))
      .to.be.revertedWith("Property ID is required");
  });

  // with no audit hash nobody could prove the AI assessment later.
  it("rejects a mortgage with no audit hash", async function () {
    await expect(loanNote.mintLoan(...approved({ auditHash: ethers.ZeroHash })))
      .to.be.revertedWith("Audit hash is required");
  });

  // a loan of 0 euro makes no sense.
  it("rejects a loan amount of zero", async function () {
    await expect(loanNote.mintLoan(...approved({ loanAmount: 0 })))
      .to.be.revertedWith("Loan amount must be more than 0");
  });

  // term must be between 1 month and 40 years.
  it("rejects a term of 0 or over 480 months", async function () {
    for (const term of [0, 481]) {
      await expect(loanNote.mintLoan(...approved({ termMonths: term })), String(term))
        .to.be.revertedWith("Term must be 1 to 480 months");
    }
  });

  // APR must be above 0 and at most 15%, anything else is a mistake.
  it("rejects an APR of 0 or over 15%", async function () {
    for (const apr of [0, 1501]) {
      await expect(loanNote.mintLoan(...approved({ apr })), String(apr))
        .to.be.revertedWith("APR must be between 0.01% and 15%");
    }
  });

  // anti-fraud: the same approval cannot be minted twice.
  it("blocks minting the same application twice", async function () {
    await loanNote.mintLoan(...approved());
    await expect(loanNote.mintLoan(...approved({ propertyId: "T12 AB34" })))
      .to.be.revertedWith("Application already minted");
  });

  // anti-fraud: no two active mortgages on the same house, even if the eircode is typed differently.
  it("blocks a second active loan on the same property", async function () {
    await loanNote.mintLoan(...approved());
    await expect(loanNote.mintLoan(...approved({ applicationId: "app-002", propertyId: "d02x285" })))
      .to.be.revertedWith("Property already has an active loan");
  });

  // once a loan is closed the house can get a new mortgage, and the old record stays.
  it("allows a new loan on a property after the old one is closed", async function () {
    await loanNote.mintLoan(...approved());
    await expect(loanNote.closeLoan(1)).to.emit(loanNote, "LoanClosed").withArgs(1, "D02X285");
    await loanNote.mintLoan(...approved({ applicationId: "app-002" }));
    expect((await loanNote.getLoan(1)).isActive).to.equal(false); //old record still there
    expect(await loanNote.activeLoanOnProperty(ethers.keccak256(ethers.toUtf8Bytes("D02X285")))).to.equal(2);
  });

  // only the owner can close a loan, and only an active one.
  it("only lets the owner close an active loan", async function () {
    await loanNote.mintLoan(...approved());
    await expect(loanNote.connect(stranger).closeLoan(1)).to.be.revertedWith("Only the owner can mint loans");
    await loanNote.closeLoan(1);
    await expect(loanNote.closeLoan(1)).to.be.revertedWith("Loan is not active");
  });

  // the audit check says true for the real assessment and false for an edited one.
  it("detects an AI assessment that was changed after approval", async function () {
    await loanNote.mintLoan(...approved());
    const edited = ethers.sha256(ethers.toUtf8Bytes("AI assessment, edited"));
    expect(await loanNote.verifyAudit(1, auditHash)).to.equal(true);
    expect(await loanNote.verifyAudit(1, edited)).to.equal(false);
  });
});
