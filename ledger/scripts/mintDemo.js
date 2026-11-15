const hre = require("hardhat");
const path = require("path");

const CONTRACT_ADDRESS = process.env.LOAN_NOTE_ADDRESS;
const SAMPLE = path.join(__dirname, "..", "samples", "approvedApplication.json");

// toMintArguments turns an approved application (same field names as firestore) into mintLoan inputs.
function toMintArguments(application) {
  const applicationData = application.applicationData;
  return [
    application.applicationId,
    application.createdBy, //firebase uid of the applicant
    application.approvedBy, //firebase uid of the underwriter
    application.bank,
    applicationData.eircode,
    Math.round(Number(applicationData.loanAmount)), //whole euro
    Number(applicationData.loanTermYears) * 12, //years to months
    application.aiAssessment.aprBasisPoints, //decided by the AI engine
    applicationData.ber,
    "0x" + application.aiAssessment.assessment_hash, //sha-256 from the AI engine
  ];
}

// printLoan reads a saved loan back from the blockchain and prints it.
async function printLoan(loanNote, id) {
  const loan = await loanNote.getLoan(id);
  console.log("Loan #" + id);
  console.log("  application:", loan.applicationId);
  console.log("  borrower:", loan.borrowerId);
  console.log("  approved by:", loan.underwriterId);
  console.log("  bank:", loan.bank);
  console.log("  property:", loan.propertyId);
  console.log("  amount: €" + loan.loanAmount.toString());
  console.log("  term (months):", loan.termMonths.toString());
  console.log("  rate:", Number(loan.apr) / 100 + "%"); //375 becomes 3.75
  console.log("  BER rating:", loan.berRating);
  console.log("  audit hash:", loan.auditHash);
  console.log("  active:", loan.isActive);
}

// main plays the underwriter: it mints the approved sample, then tries the same house again.
async function main() {
  if (!CONTRACT_ADDRESS) {
    throw new Error("Set LOAN_NOTE_ADDRESS to the address printed by scripts/deploy.js.");
  }

  const [bank] = await hre.ethers.getSigners(); //account 0 is the bank
  const loanNote = await hre.ethers.getContractAt("LoanNote", CONTRACT_ADDRESS, bank);
  const application = require(SAMPLE);

  if (application.status !== "Approved") {
    throw new Error("Only approved applications can be minted.");
  }

  await (await loanNote.mintLoan(...toMintArguments(application))).wait();
  const id = await loanNote.loanCount();
  await printLoan(loanNote, id);

  // anti-fraud check: a second application on the same house must be rejected
  const copy = { ...application, applicationId: application.applicationId + "-copy" };
  try {
    await loanNote.mintLoan.staticCall(...toMintArguments(copy));
    console.log("Second loan on the same house was allowed (this should not happen)");
  } catch (error) {
    console.log("Second loan on the same house rejected:", error.reason || error.message);
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
