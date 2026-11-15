# Loan-note ledger

Hardhat/Solidity prototype for recording approved mortgages on an EVM chain.
The ledger does no calculations: the AI engine decides the APR (including the
BER green discount) and the underwriter approves. `LoanNote` then checks the
record and stores it for good.

Each loan note stores: application ID, borrower ID, underwriter ID, bank,
property ID (Eircode), loan amount, term, APR (basis points), BER rating, the
AI assessment's SHA-256 audit hash, a timestamp, and whether it is active.

Rules enforced on minting:

- Only the deploying (bank) account can mint or close loans.
- All IDs, the bank and the audit hash are required.
- Loan amount above 0, term 1 to 480 months, APR 0.01% to 15%.
- Anti-fraud: one application can only be minted once, and a property can
  only have one active loan. Eircodes are compared without spaces and in
  capitals, so `d02 x285` and `D02X285` are the same property.
- `closeLoan` frees the property for a new mortgage; the old record stays.
- `verifyAudit` shows whether an AI assessment matches the recorded hash.

The ledger is not yet called by the web app. `scripts/mintDemo.js` mints
`samples/approvedApplication.json`, which uses the same field names as the
web app's Firestore `applications` record plus the AI engine's
`assessment_hash`. All values in it are synthetic demo data.

## Requirements

- Node.js and npm

## Install and test (PowerShell)

```powershell
cd ledger
npm.cmd install
npm.cmd test
npm.cmd run compile
```

## Deploy and run the demo on a local Hardhat node

Use two PowerShell terminals. In terminal 1:

```powershell
cd ledger
npx.cmd hardhat node
```

In terminal 2:

```powershell
cd ledger
npx.cmd hardhat run scripts/deploy.js --network localhost
$env:LOAN_NOTE_ADDRESS = "paste the deployed address printed above"
npx.cmd hardhat run scripts/mintDemo.js --network localhost
```

The demo requires `LOAN_NOTE_ADDRESS` because each deployment can have a
different address. The local Hardhat chain is ephemeral; restart it and deploy
again to reset the demo.

## Network and production notes

Only the local Hardhat and localhost networks are configured. Add and review
any external network configuration and deployment key management separately;
never commit private keys. The contract is not a
production lending mechanism.
