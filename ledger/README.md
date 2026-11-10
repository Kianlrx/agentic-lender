# Loan-note ledger

Hardhat/Solidity prototype for recording approved loan notes on an EVM chain.
`LoanNote` stores a loan amount, term, BER rating, APR, and audit hash. Only
the deploying account can mint. BER A/B ratings get a fixed 0.25 percentage
point discount from the prototype's 5.00% base APR.

This contract is not connected to the web app, mobile app, or AI engine. The
loan values and audit hash in `mintDemo.js` are synthetic demo data.

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
never commit private keys. The contract hardcodes the demo APR and is not a
production lending or rate-setting mechanism.
