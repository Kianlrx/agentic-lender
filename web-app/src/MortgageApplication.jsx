import React, { useRef, useState } from 'react';
import './MortgageApplication.css';

const steps = [
  { title: 'Setup', subtitle: 'Application details' },
  { title: 'Identity', subtitle: 'Applicant details' },
  { title: 'Work & income', subtitle: 'Income details' },
  { title: 'Assets & debts', subtitle: 'Financial commitments' },
  { title: 'Property', subtitle: 'Property details' },
  { title: 'Loan request', subtitle: 'Mortgage details' },
  { title: 'Costs & funds', subtitle: 'Purchase funding' },
  { title: 'Docs & consent', subtitle: 'Documents and declarations' }
];

const createApplicant = () => ({
  legalName: '',
  previousName: '',
  dateOfBirth: '',
  gender: '',
  ppsn: '',
  maritalStatus: '',
  nationality: '',
  residency: '',
  yearsInIreland: '',
  nonEea: '',
  visaType: '',
  visaExpiry: '',
  email: '',
  phone: '',
  addressHistory: [{ address: '', eircode: '', from: '', to: '' }],
  dependantsCount: '0',
  dependantAges: '',
  accommodation: '',
  monthlyRent: '',
  employmentCategory: '',
  employer: '',
  occupation: '',
  employmentStart: '',
  contractType: '',
  yearsWithEmployer: '',
  previousEmployer: '',
  grossAnnualSalary: '',
  netMonthlyPay: '',
  payFrequency: '',
  overtime: '',
  bonus: '',
  commission: '',
  variablePayRegularity: '',
  otherIncome: [{ source: '', amount: '', duration: '' }],
  incomeCurrency: 'EUR',
  otherCurrency: '',
  businessName: '',
  ownershipPercentage: '',
  netProfitYear1: '',
  netProfitYear2: '',
  accountant: '',
  bankAccounts: [{ provider: '', balance: '', monthlySaving: '', source: '' }],
  otherAssets: [{ description: '', value: '' }],
  loans: [{ lender: '', balance: '', repayment: '', monthsRemaining: '', refinance: '' }],
  creditCards: [{ provider: '', limit: '', balance: '' }],
  maintenanceAndChildcare: ''
});

const createInitialForm = () => ({
  applicationType: '',
  applicationKind: '',
  propertyFound: '',
  applicants: [createApplicant()],
  existingProperties: [{ address: '', mortgageBalance: '', monthlyRepayment: '', forbearance: '' }],
  insolvency: '',
  insolvencyDetails: '',
  futureChanges: '',
  propertyAddress: '',
  eircode: '',
  propertyType: '',
  propertyStatus: '',
  yearBuilt: '',
  ber: '',
  floorArea: '',
  bedrooms: '',
  tenure: '',
  propertyUse: '',
  otherOccupants: '',
  siteValue: '',
  buildCost: '',
  stagePayments: '',
  expectedRent: '',
  loanAmount: '',
  loanTermYears: '',
  rateType: '',
  fixedTermYears: '',
  repaymentType: '',
  purchasePrice: '',
  stampDuty: '',
  legalCosts: '',
  renovationCosts: '',
  funds: [{ source: '', amount: '', donorRelationship: '' }],
  solicitorName: '',
  solicitorFirm: '',
  solicitorEmail: '',
  solicitorPhone: '',
  declarations: {
    accurate: false,
    creditCheck: false,
    dataProcessing: false,
    aiAnalysis: false
  }
});

const fieldClass = 'mortgage-field';
const selectOptions = {
  applicationType: ['Purchase', 'Switch mortgage', 'Remortgage', 'Mortgage top-up'],
  applicationKind: ['Single applicant', 'Joint applicants'],
  yesNo: ['Yes', 'No'],
  maritalStatus: ['Single', 'Married', 'Civil partnership', 'Separated', 'Divorced', 'Widowed'],
  gender: ['Female', 'Male', 'Non-binary', 'Prefer not to say'],
  residency: ['Irish citizen', 'EU/EEA citizen', 'Non-EEA resident'],
  accommodation: ['Owner-occupier', 'Renting', 'Living with family', 'Other'],
  employmentCategory: ['Employed', 'Self-employed', 'Unemployed', 'Retired'],
  contractType: ['Permanent', 'Fixed term', 'Contract', 'Part-time', 'Other'],
  payFrequency: ['Weekly', 'Fortnightly', 'Monthly', 'Annually'],
  regularity: ['Regular', 'Occasional'],
  propertyType: ['House', 'Apartment', 'Duplex', 'Other'],
  propertyStatus: ['New build', 'Second-hand', 'Self-build', 'Buy-to-let'],
  tenure: ['Freehold', 'Leasehold', 'Not sure'],
  propertyUse: ['Home to live in', 'Buy-to-let', 'Holiday home'],
  rateType: ['Fixed', 'Variable', 'Split'],
  repaymentType: ['Principal and interest', 'Interest only', 'Part and part'],
  incomeCurrency: ['EUR', 'GBP', 'USD', 'Other'],
  refinance: ['Yes', 'No']
};

const readPath = (object, path) => path.reduce((value, key) => value?.[key], object);

const updatePath = (object, path, value) => {
  const [key, ...remaining] = path;
  if (Array.isArray(object)) {
    return object.map((item, index) => index === key
      ? remaining.length ? updatePath(item, remaining, value) : value
      : item);
  }
  return {
    ...object,
    [key]: remaining.length ? updatePath(object[key], remaining, value) : value
  };
};

const formatEuro = (amount) => new Intl.NumberFormat('en-IE', {
  style: 'currency',
  currency: 'EUR',
  maximumFractionDigits: 0
}).format(amount);

export default function MortgageApplication({ onCancel, onSubmit, submitting, error }) {
  const [form, setForm] = useState(createInitialForm);
  const [files, setFiles] = useState({});
  const [step, setStep] = useState(0);
  const [formError, setFormError] = useState('');
  const formRef = useRef(null);

  const update = (path, value) => setForm(current => {
    let updated = updatePath(current, path, value);
    if (path.length === 1 && path[0] === 'applicationKind') {
      updated = updatePath(updated, ['applicants'], value === 'Joint applicants'
        ? updated.applicants.length === 1 ? [...updated.applicants, createApplicant()] : updated.applicants.slice(0, 2)
        : updated.applicants.slice(0, 1));
    }
    return updated;
  });
  const read = (path) => readPath(form, path);

  const renderField = (label, path, options = {}) => {
    const value = read(path) ?? '';
    const choices = options.options || (options.type === 'yesNo' ? selectOptions.yesNo : null);
    return (
      <label className={fieldClass} key={path.join('.')}>
        <span>
          {label}{options.unit ? ` (${options.unit})` : ''}
          {options.required ? <span className="mortgage-required-marker"> *</span> : null}
        </span>
        {choices ? (
          <span className="mortgage-select-wrap">
            <select
              value={value}
              onChange={event => update(path, event.target.value)}
              required={options.required}
            >
              <option value="">Select an option</option>
              {choices.map(choice => <option key={choice} value={choice}>{choice}</option>)}
            </select>
          </span>
        ) : (
          <input
            type={options.type || 'text'}
            value={value}
            onChange={event => update(path, event.target.value)}
            required={options.required}
            min={options.min}
            max={options.max}
            step={options.step}
            maxLength={options.maxLength}
            autoComplete={options.autoComplete}
          />
        )}
        {options.help ? <small>{options.help}</small> : null}
      </label>
    );
  };

  const renderCheck = (label, path, required = false) => (
    <label className="mortgage-check" key={path.join('.')}>
      <input
        type="checkbox"
        checked={Boolean(read(path))}
        onChange={event => update(path, event.target.checked)}
        required={required}
      />
      <span>{label}{required ? ' *' : ''}</span>
    </label>
  );

  const renderRepeatGroup = (title, path, fields, { itemLabel = title, addLabel = `Add ${title.toLowerCase()}` } = {}) => {
    const items = read(path) || [];
    return (
      <div className="mortgage-repeat-group" key={path.join('.')}>
        <h3>{title}</h3>
        {items.map((item, index) => (
          <fieldset className="mortgage-repeat-item" key={`${path.join('.')}-${index}`}>
            <legend>{itemLabel} {index + 1}.</legend>
            <div className="mortgage-form-grid">
              {fields.map(field => renderField(field.label, [...path, index, field.key], field.options || {}))}
            </div>
            {items.length > 1 ? (
              <button
                type="button"
                className="secondary-button mortgage-remove-button"
                onClick={() => update(path, items.filter((_, itemIndex) => itemIndex !== index))}
              >
                Remove
              </button>
            ) : null}
          </fieldset>
        ))}
        <button
          type="button"
          className="secondary-button"
          onClick={() => update(path, [...items, Object.fromEntries(fields.map(field => [field.key, '']))])}
        >
          {addLabel}
        </button>
      </div>
    );
  };

  const addApplicant = () => {
    if (form.applicants.length < 2) {
      update(['applicants'], [...form.applicants, createApplicant()]);
    }
  };

  const removeApplicant = (index) => {
    update(['applicants'], form.applicants.filter((_, applicantIndex) => applicantIndex !== index));
  };

  const handleFiles = (category, event) => {
    const selected = Array.from(event.target.files || []);
    const invalidFile = selected.find(file =>
      file.size > 15 * 1024 * 1024
      || !['application/pdf', 'image/jpeg', 'image/png'].includes(file.type)
    );
    if (invalidFile) {
      setFormError(`${invalidFile.name} must be a PDF, JPEG or PNG file no larger than 15 MB.`);
      event.target.value = '';
      return;
    }
    setFormError('');
    setFiles(current => ({ ...current, [category]: selected }));
    event.target.value = '';
  };

  const nextStep = () => {
    if (!formRef.current?.reportValidity()) return;
    setStep(current => Math.min(current + 1, steps.length - 1));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const previousStep = () => {
    setStep(current => Math.max(current - 1, 0));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const getStepContent = () => {
    if (step === 0) {
      return (
        <>
          <p className="mortgage-step-intro">Tell us what you are applying for. Your answers help us show the right questions as you go.</p>
          <div className="mortgage-form-grid">
            {renderField('Application type', ['applicationType'], { options: selectOptions.applicationType, required: true })}
            {renderField('Applicants', ['applicationKind'], { options: selectOptions.applicationKind, required: true })}
            {renderField('Have you found a property?', ['propertyFound'], { type: 'yesNo', required: true })}
          </div>
          <div className="mortgage-note">
            <strong>Not found a property yet?</strong>
            <span>You can continue with an Agreement in Principle and add property details later.</span>
          </div>
        </>
      );
    }

    if (step === 1) {
      return (
        <>
          <p className="mortgage-step-intro">We need these details for each person applying for the mortgage.</p>
          {form.applicants.map((applicant, index) => {
            const applicantPath = ['applicants', index];
            return (
              <fieldset
                className="mortgage-applicant"
                key={`applicant-${index}`}
                aria-label={form.applicationKind === 'Joint applicants' ? undefined : 'Applicant details'}
              >
                {form.applicationKind === 'Joint applicants' ? <legend>Applicant {index + 1}.</legend> : null}
                <div className="mortgage-form-grid">
                  {renderField('Legal name', [...applicantPath, 'legalName'], { required: true, autoComplete: 'name' })}
                  {renderField('Previous / maiden name', [...applicantPath, 'previousName'])}
                  {renderField('Date of birth', [...applicantPath, 'dateOfBirth'], { type: 'date', required: true })}
                  {renderField('Gender (optional)', [...applicantPath, 'gender'], { options: selectOptions.gender })}
                  {renderField('PPSN', [...applicantPath, 'ppsn'], { help: 'Your PPS number is stored with this application.' })}
                  {renderField('Marital / civil status', [...applicantPath, 'maritalStatus'], { options: selectOptions.maritalStatus, required: true })}
                  {renderField('Nationality', [...applicantPath, 'nationality'], { required: true })}
                  {renderField('Residency status', [...applicantPath, 'residency'], { options: selectOptions.residency, required: true })}
                  {renderField('Years living in Ireland', [...applicantPath, 'yearsInIreland'], { type: 'number', min: 0, step: 1, required: true })}
                  {renderField('Non-EEA national?', [...applicantPath, 'nonEea'], { type: 'yesNo', required: true })}
                  {applicant.nonEea === 'Yes' ? (
                    <>
                      {renderField('Visa / IRP stamp', [...applicantPath, 'visaType'], { required: true })}
                      {renderField('Visa / IRP expiry date', [...applicantPath, 'visaExpiry'], { type: 'date', required: true })}
                    </>
                  ) : null}
                  {renderField('Email address', [...applicantPath, 'email'], { type: 'email', required: true, autoComplete: 'email' })}
                  {renderField('Phone number', [...applicantPath, 'phone'], { type: 'tel', required: true, autoComplete: 'tel' })}
                  {renderField('Number of dependants', [...applicantPath, 'dependantsCount'], { type: 'number', min: 0, step: 1, required: true })}
                  {Number(applicant.dependantsCount) > 0 ? renderField('Dependants’ ages', [...applicantPath, 'dependantAges'], { help: 'Enter ages separated by commas.', required: true }) : null}
                  {renderField('Current accommodation', [...applicantPath, 'accommodation'], { options: selectOptions.accommodation, required: true })}
                  {applicant.accommodation === 'Renting' ? renderField('Monthly rent', [...applicantPath, 'monthlyRent'], { type: 'number', min: 0, step: '0.01', unit: 'EUR', required: true }) : null}
                </div>
                {renderRepeatGroup('Address history (last 3 years)', [...applicantPath, 'addressHistory'], [
                  { key: 'address', label: 'Address', options: { required: true } },
                  { key: 'eircode', label: 'Eircode' },
                  { key: 'from', label: 'From', options: { type: 'date' } },
                  { key: 'to', label: 'To', options: { type: 'date' } }
                ], { itemLabel: 'Address', addLabel: 'Add address history' })}
                {index > 0 ? (
                  <button type="button" className="secondary-button mortgage-remove-button" onClick={() => removeApplicant(index)}>
                    Remove applicant
                  </button>
                ) : null}
              </fieldset>
            );
          })}
          {form.applicationKind === 'Joint applicants' && form.applicants.length < 2 ? (
            <button type="button" className="secondary-button" onClick={addApplicant}>+ Add joint applicant</button>
          ) : null}
        </>
      );
    }

    if (step === 2) {
      return (
        <>
          <p className="mortgage-step-intro">Add employment and income information for each applicant. Self-employed details appear when selected.</p>
          {form.applicants.map((applicant, index) => {
            const path = ['applicants', index];
            return (
              <fieldset
                className="mortgage-applicant"
                key={`income-${index}`}
                aria-label={form.applicationKind === 'Joint applicants' ? undefined : 'Applicant income details'}
              >
                {form.applicationKind === 'Joint applicants' ? <legend>Applicant {index + 1}.</legend> : null}
                <div className="mortgage-form-grid">
                  {renderField('Employment / tax category', [...path, 'employmentCategory'], { options: selectOptions.employmentCategory, required: true })}
                  {['Employed', 'Self-employed'].includes(applicant.employmentCategory) ? (
                    <>
                      {renderField(applicant.employmentCategory === 'Self-employed' ? 'Business name' : 'Employer', [...path, applicant.employmentCategory === 'Self-employed' ? 'businessName' : 'employer'], { required: true })}
                      {renderField('Occupation', [...path, 'occupation'], { required: true })}
                      {renderField('Employment start date', [...path, 'employmentStart'], { type: 'date', required: applicant.employmentCategory === 'Employed' })}
                      {applicant.employmentCategory === 'Employed' ? renderField('Contract type', [...path, 'contractType'], { options: selectOptions.contractType, required: true }) : null}
                      {renderField('Years with current employer', [...path, 'yearsWithEmployer'], { type: 'number', min: 0, step: 1, required: true })}
                      {applicant.yearsWithEmployer !== '' && Number(applicant.yearsWithEmployer) < 2 ? renderField('Previous employer', [...path, 'previousEmployer'], { required: true }) : null}
                    </>
                  ) : null}
                  {renderField('Gross basic annual salary', [...path, 'grossAnnualSalary'], { type: 'number', min: 0, step: '0.01', unit: 'EUR', required: ['Employed', 'Self-employed'].includes(applicant.employmentCategory) })}
                  {renderField('Net monthly pay', [...path, 'netMonthlyPay'], { type: 'number', min: 0, step: '0.01', unit: 'EUR', required: ['Employed', 'Self-employed'].includes(applicant.employmentCategory) })}
                  {renderField('Pay frequency', [...path, 'payFrequency'], { options: selectOptions.payFrequency, required: ['Employed', 'Self-employed'].includes(applicant.employmentCategory) })}
                  {renderField('Overtime amount', [...path, 'overtime'], { type: 'number', min: 0, step: '0.01', unit: 'EUR' })}
                  {renderField('Bonus amount', [...path, 'bonus'], { type: 'number', min: 0, step: '0.01', unit: 'EUR' })}
                  {renderField('Commission amount', [...path, 'commission'], { type: 'number', min: 0, step: '0.01', unit: 'EUR' })}
                  {renderField('Overtime / bonus regularity', [...path, 'variablePayRegularity'], { options: selectOptions.regularity })}
                  {renderField('Income currency', [...path, 'incomeCurrency'], { options: selectOptions.incomeCurrency, required: true })}
                  {applicant.incomeCurrency === 'Other' ? renderField('Other income currency', [...path, 'otherCurrency'], { required: true }) : null}
                  {applicant.employmentCategory === 'Self-employed' ? (
                    <>
                      {renderField('Business ownership', [...path, 'ownershipPercentage'], { type: 'number', min: 0, max: 100, step: 0.1, unit: '%', required: true })}
                      {renderField('Net profit - year 1', [...path, 'netProfitYear1'], { type: 'number', min: 0, step: '0.01', unit: 'EUR', required: true })}
                      {renderField('Net profit - year 2', [...path, 'netProfitYear2'], { type: 'number', min: 0, step: '0.01', unit: 'EUR', required: true })}
                      {renderField('Accountant name', [...path, 'accountant'])}
                    </>
                  ) : null}
                </div>
                {renderRepeatGroup('Other / rental income', [...path, 'otherIncome'], [
                  { key: 'source', label: 'Income source' },
                  { key: 'amount', label: 'Amount', options: { type: 'number', min: 0, step: '0.01', unit: 'EUR' } },
                  { key: 'duration', label: 'Duration / regularity' }
                ], { itemLabel: 'Other income', addLabel: 'Add other income' })}
              </fieldset>
            );
          })}
        </>
      );
    }

    if (step === 3) {
      return (
        <>
          <p className="mortgage-step-intro">List savings, assets, credit commitments and any existing properties for each applicant.</p>
          {form.applicants.map((applicant, index) => {
            const path = ['applicants', index];
            return (
              <fieldset
                className="mortgage-applicant"
                key={`assets-${index}`}
                aria-label={form.applicationKind === 'Joint applicants' ? undefined : 'Applicant assets and commitments'}
              >
                {form.applicationKind === 'Joint applicants' ? <legend>Applicant {index + 1}.</legend> : null}
                {renderRepeatGroup('Bank and savings accounts', [...path, 'bankAccounts'], [
                  { key: 'provider', label: 'Bank / provider' },
                  { key: 'balance', label: 'Balance', options: { type: 'number', min: 0, step: '0.01', unit: 'EUR' } },
                  { key: 'monthlySaving', label: 'Monthly saving', options: { type: 'number', min: 0, step: '0.01', unit: 'EUR' } },
                  { key: 'source', label: 'Source of funds' }
                ], { itemLabel: 'Bank account', addLabel: 'Add bank account' })}
                {renderRepeatGroup('Other assets (shares, policies, land)', [...path, 'otherAssets'], [
                  { key: 'description', label: 'Asset description' },
                  { key: 'value', label: 'Estimated value', options: { type: 'number', min: 0, step: '0.01', unit: 'EUR' } }
                ], { itemLabel: 'Other asset', addLabel: 'Add other asset' })}
                {renderRepeatGroup('Loans and credit commitments', [...path, 'loans'], [
                  { key: 'lender', label: 'Lender' },
                  { key: 'balance', label: 'Outstanding balance', options: { type: 'number', min: 0, step: '0.01', unit: 'EUR' } },
                  { key: 'repayment', label: 'Monthly repayment', options: { type: 'number', min: 0, step: '0.01', unit: 'EUR' } },
                  { key: 'monthsRemaining', label: 'Months remaining', options: { type: 'number', min: 0, step: 1 } },
                  { key: 'refinance', label: 'To be refinanced?', options: { type: 'yesNo' } }
                ], { itemLabel: 'Loan', addLabel: 'Add loan' })}
                {renderRepeatGroup('Credit cards', [...path, 'creditCards'], [
                  { key: 'provider', label: 'Provider' },
                  { key: 'limit', label: 'Credit limit', options: { type: 'number', min: 0, step: '0.01', unit: 'EUR' } },
                  { key: 'balance', label: 'Current balance', options: { type: 'number', min: 0, step: '0.01', unit: 'EUR' } }
                ], { itemLabel: 'Credit card', addLabel: 'Add credit card' })}
                <div className="mortgage-form-grid">
                  {renderField('Maintenance and childcare costs', [...path, 'maintenanceAndChildcare'], { type: 'number', min: 0, step: '0.01', unit: 'EUR / month' })}
                </div>
              </fieldset>
            );
          })}
          {renderRepeatGroup('Existing properties and mortgages', ['existingProperties'], [
            { key: 'address', label: 'Property address' },
            { key: 'mortgageBalance', label: 'Mortgage balance', options: { type: 'number', min: 0, step: '0.01', unit: 'EUR' } },
            { key: 'monthlyRepayment', label: 'Monthly repayment', options: { type: 'number', min: 0, step: '0.01', unit: 'EUR' } },
            { key: 'forbearance', label: 'Forbearance arrangements?', options: { type: 'yesNo' } }
          ], { itemLabel: 'Existing property', addLabel: 'Add existing property' })}
          <div className="mortgage-form-grid">
            {renderField('Insolvency or judgements?', ['insolvency'], { type: 'yesNo', required: true })}
            {form.insolvency === 'Yes' ? renderField('Please provide details', ['insolvencyDetails'], { required: true }) : null}
            {renderField('Expected future changes to finances?', ['futureChanges'], { type: 'yesNo' })}
          </div>
        </>
      );
    }

    if (step === 4) {
      return (
        <>
          <p className="mortgage-step-intro">
            {form.propertyFound === 'No'
              ? 'You can continue without an address. Property details can be added when you have found a home.'
              : 'Tell us about the property you would like to buy or refinance.'}
          </p>
          {form.propertyFound === 'Yes' ? (
            <div className="mortgage-form-grid">
              {renderField('Property address', ['propertyAddress'], { required: true })}
              {renderField('Eircode', ['eircode'], { required: true })}
              {renderField('Property type', ['propertyType'], { options: selectOptions.propertyType, required: true })}
              {renderField('New or existing property', ['propertyStatus'], { options: selectOptions.propertyStatus, required: true })}
              {renderField('Year built', ['yearBuilt'], { type: 'number', min: 1700, max: new Date().getFullYear(), step: 1 })}
              {renderField('BER rating', ['ber'])}
              {renderField('Floor area', ['floorArea'], { type: 'number', min: 0, step: '0.1', unit: 'm²' })}
              {renderField('Bedrooms', ['bedrooms'], { type: 'number', min: 0, step: 1 })}
              {renderField('Freehold / leasehold', ['tenure'], { options: selectOptions.tenure })}
              {renderField('Property use', ['propertyUse'], { options: selectOptions.propertyUse, required: true })}
              {renderField('Other occupants', ['otherOccupants'])}
              {form.propertyStatus === 'Self-build' ? (
                <>
                  {renderField('Site value', ['siteValue'], { type: 'number', min: 0, step: '0.01', unit: 'EUR', required: true })}
                  {renderField('Estimated build cost', ['buildCost'], { type: 'number', min: 0, step: '0.01', unit: 'EUR', required: true })}
                  {renderField('Stage payments', ['stagePayments'], { type: 'text', help: 'Outline expected payment stages.' })}
                </>
              ) : null}
              {form.propertyUse === 'Buy-to-let' ? renderField('Expected monthly rent', ['expectedRent'], { type: 'number', min: 0, step: '0.01', unit: 'EUR', required: true }) : null}
            </div>
          ) : (
            <div className="mortgage-note"><strong>Property details are optional at this stage.</strong><span>Continue to your loan request and provide an estimated purchase price if available.</span></div>
          )}
        </>
      );
    }

    if (step === 5) {
      const allIncomeInEuro = form.applicants.every(applicant => applicant.incomeCurrency === 'EUR');
      const combinedIncome = allIncomeInEuro
        ? form.applicants.reduce((total, applicant) => total + Number(applicant.grossAnnualSalary || 0), 0)
        : 0;
      const propertyValue = Number(form.purchasePrice || 0);
      const loanAmount = Number(form.loanAmount || 0);
      const ltv = propertyValue > 0 ? (loanAmount / propertyValue) * 100 : null;
      const lti = combinedIncome > 0 ? loanAmount / combinedIncome : null;
      return (
        <>
          <p className="mortgage-step-intro">Choose the amount, term and repayment structure you are looking for.</p>
          <div className="mortgage-form-grid">
            {renderField('Loan amount requested', ['loanAmount'], { type: 'number', min: 1, step: '0.01', unit: 'EUR', required: true })}
            {renderField('Mortgage term', ['loanTermYears'], { type: 'number', min: 1, max: 40, step: 1, unit: 'years', required: true })}
            {renderField('Rate type', ['rateType'], { options: selectOptions.rateType, required: true })}
            {form.rateType === 'Fixed' || form.rateType === 'Split' ? renderField('Fixed rate period', ['fixedTermYears'], { type: 'number', min: 1, max: 40, step: 1, unit: 'years', required: true }) : null}
            {renderField('Repayment type', ['repaymentType'], { options: selectOptions.repaymentType, required: true })}
          </div>
          <div className="mortgage-derived">
            <div><span>Loan-to-value</span><strong>{ltv === null ? 'Add a purchase price to calculate' : `${ltv.toFixed(1)}%`}</strong></div>
            <div><span>Loan-to-income</span><strong>{lti === null ? allIncomeInEuro ? 'Add income details to calculate' : 'Use EUR for all incomes to calculate' : `${lti.toFixed(2)}×`}</strong></div>
            <small>Indicative calculations only; your lender will confirm affordability and loan limits.</small>
          </div>
        </>
      );
    }

    if (step === 6) {
      const outlay = ['purchasePrice', 'stampDuty', 'legalCosts', 'renovationCosts']
        .reduce((total, key) => total + Number(form[key] || 0), 0);
      const funding = form.funds.reduce((total, fund) => total + Number(fund.amount || 0), 0);
      const remaining = outlay - Number(form.loanAmount || 0) - funding;
      return (
        <>
          <p className="mortgage-step-intro">Enter the purchase costs and explain where the deposit and other funds are coming from.</p>
          <div className="mortgage-form-grid">
            {renderField('Purchase price / current value', ['purchasePrice'], { type: 'number', min: 0, step: '0.01', unit: 'EUR', required: true })}
            {renderField('Stamp duty', ['stampDuty'], { type: 'number', min: 0, step: '0.01', unit: 'EUR' })}
            {renderField('Legal costs', ['legalCosts'], { type: 'number', min: 0, step: '0.01', unit: 'EUR' })}
            {renderField('Renovation costs', ['renovationCosts'], { type: 'number', min: 0, step: '0.01', unit: 'EUR' })}
          </div>
          {renderRepeatGroup('Sources of funds', ['funds'], [
            { key: 'source', label: 'Source (savings, gift, inheritance, sale proceeds, Help to Buy)' },
            { key: 'amount', label: 'Amount', options: { type: 'number', min: 0, step: '0.01', unit: 'EUR' } },
            { key: 'donorRelationship', label: 'Donor relationship (if gift)' }
          ], { itemLabel: 'Source of funds', addLabel: 'Add source of funds' })}
          {form.funds.some(fund => (fund.source || '').toLowerCase().includes('gift')) ? (
            <div className="mortgage-note"><strong>Gift letter required</strong><span>Please include a gift letter in the documents section.</span></div>
          ) : null}
          <div className={`mortgage-balance ${Math.abs(remaining) < 1 && outlay > 0 && Number(form.loanAmount) > 0 ? 'is-balanced' : 'is-unbalanced'}`} aria-live="polite">
            <span>Costs: {formatEuro(outlay)}</span>
            <span>Loan + funds: {formatEuro(Number(form.loanAmount || 0) + funding)}</span>
            <strong>
              {outlay === 0 || Number(form.loanAmount) === 0
                ? 'Enter a purchase price and loan amount to check the balance.'
                : Math.abs(remaining) < 1 ? 'Your totals balance.' : `Difference: ${formatEuro(Math.abs(remaining))}`}
            </strong>
          </div>
        </>
      );
    }

    return (
      <>
        <p className="mortgage-step-intro">Add the supporting documents you have available, solicitor details, and required declarations.</p>
        <div className="mortgage-documents">
          {[
            ['identity', 'ID, proof of address and PPSN proof'],
            ['income', 'Payslips, salary certificate and Employment Detail Summary'],
            ['bankStatements', 'Six months of account statements'],
            ['additional', 'Conditional documents (Forms 11, gift letter, IRP, separation agreement)']
          ].map(([category, label]) => (
            <label className="mortgage-upload" key={category}>
              <span>{label}</span>
              <input
                type="file"
                multiple
                accept=".pdf,.jpg,.jpeg,.png"
                onChange={event => handleFiles(category, event)}
              />
              <small>PDF or image, up to 15 MB per file</small>
              {files[category]?.length ? (
                <ul>{files[category].map(file => <li key={`${file.name}-${file.lastModified}`}>{file.name}</li>)}</ul>
              ) : null}
            </label>
          ))}
        </div>
        <fieldset className="mortgage-solicitor">
          <legend>Solicitor details</legend>
          <div className="mortgage-form-grid">
            {renderField('Solicitor name', ['solicitorName'])}
            {renderField('Firm', ['solicitorFirm'])}
            {renderField('Email', ['solicitorEmail'], { type: 'email' })}
            {renderField('Phone', ['solicitorPhone'], { type: 'tel' })}
          </div>
        </fieldset>
        <div className="mortgage-declarations">
          <h3>Declarations and consent</h3>
          {renderCheck('I confirm the information provided is complete and accurate.', ['declarations', 'accurate'], true)}
          {renderCheck('I consent to the lender carrying out credit checks.', ['declarations', 'creditCheck'], true)}
          {renderCheck('I consent to the processing of my personal data to assess this application.', ['declarations', 'dataProcessing'], true)}
          {renderCheck('I consent to AI-assisted analysis of my application.', ['declarations', 'aiAnalysis'])}
        </div>
      </>
    );
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    if (step < steps.length - 1) {
      nextStep();
      return;
    }
    const outlay = ['purchasePrice', 'stampDuty', 'legalCosts', 'renovationCosts']
      .reduce((total, key) => total + Number(form[key] || 0), 0);
    const funding = form.funds.reduce((total, fund) => total + Number(fund.amount || 0), 0);
    if (Math.abs(outlay - Number(form.loanAmount || 0) - funding) >= 1) {
      setFormError('Please make sure your loan and funding sources balance the total costs before submitting.');
      return;
    }
    setFormError('');
    onSubmit(form, files);
  };

  return (
    <section className="mortgage-application">
      <div className="mortgage-title-row">
        <div>
          <h2 className="page-heading">Mortgage application</h2>
          <p>Complete each section to send your application for review.</p>
        </div>
      </div>
      <div className="mortgage-progress" aria-label={`Step ${step + 1} of ${steps.length}`}>
        <div className="mortgage-progress-copy">
          <span>Step {step + 1} of {steps.length}</span>
          <strong>{steps[step].title}</strong>
          <span>{Math.round(((step + 1) / steps.length) * 100)}% complete</span>
        </div>
        <div
          className="mortgage-progress-track"
          role="progressbar"
          aria-label="Application progress"
          aria-valuemin="1"
          aria-valuemax={steps.length}
          aria-valuenow={step + 1}
          aria-valuetext={`Step ${step + 1} of ${steps.length}: ${steps[step].title}`}
        >
          <div className="mortgage-progress-fill" style={{ width: `${((step + 1) / steps.length) * 100}%` }} />
        </div>
        <ol className="mortgage-step-list">
          {steps.map((item, index) => (
            <li key={item.title} className={index === step ? 'is-current' : index < step ? 'is-complete' : ''}>
              <span className="mortgage-step-number">{index < step ? '✓' : index + 1}</span>
              <span className="mortgage-step-name">{item.title}</span>
            </li>
          ))}
        </ol>
      </div>
      <form ref={formRef} onSubmit={handleSubmit} className="mortgage-form" noValidate={false}>
        <div className="mortgage-section-heading">
          <span>SECTION {step + 1}</span>
          <h3>{steps[step].title}</h3>
          <p>{steps[step].subtitle}</p>
        </div>
        <div className="mortgage-step-content" key={step}>
          {getStepContent()}
        </div>
        {formError ? <p className="error-message" role="alert">{formError}</p> : null}
        {error ? <p className="error-message" role="alert">{error}</p> : null}
        <div className="mortgage-form-actions">
          {step > 0 ? <button type="button" className="secondary-button" onClick={previousStep} disabled={submitting}>Back</button> : null}
          <button type="button" className="secondary-button mortgage-cancel-button" onClick={onCancel} disabled={submitting}>Cancel</button>
          {step < steps.length - 1 ? (
            <button type="button" className="primary-button" onClick={nextStep}>Continue</button>
          ) : (
            <button type="submit" className="primary-button" disabled={submitting}>
              {submitting ? 'Submitting application...' : 'Submit application'}
            </button>
          )}
        </div>
        <p className="mortgage-required-note">* Required fields</p>
      </form>
    </section>
  );
}
