import React, { useEffect, useRef, useState } from 'react';
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
  addressHistory: [{ address: '', eircode: '', from: '', to: '', current: false }],
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
  loans: [],
  creditCards: [],
  maintenanceAndChildcare: ''
});

const createInitialForm = () => ({
  applicationType: '',
  applicationKind: '',
  propertyFound: '',
  applicants: [createApplicant()],
  existingProperties: [],
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
const nationalities = `Afghan|Albanian|Algerian|American|Andorran|Angolan|Argentine|Armenian|Australian|Austrian|Azerbaijani|Bahamian|Bahraini|Bangladeshi|Barbadian|Belarusian|Belgian|Belizean|Beninese|Bhutanese|Bolivian|Bosnian|Brazilian|British|Bulgarian|Burkinabé|Burmese|Burundian|Cambodian|Cameroonian|Canadian|Cape Verdean|Chadian|Chilean|Chinese|Colombian|Congolese|Costa Rican|Croatian|Cuban|Cypriot|Czech|Danish|Djiboutian|Dominican|Dutch|Ecuadorian|Egyptian|English|Eritrean|Estonian|Ethiopian|Fijian|Finnish|French|Gambian|Georgian|German|Ghanaian|Greek|Grenadian|Guatemalan|Guinean|Guyanese|Haitian|Honduran|Hungarian|Icelandic|Indian|Indonesian|Iranian|Iraqi|Irish|Israeli|Italian|Ivorian|Jamaican|Japanese|Jordanian|Kazakh|Kenyan|Kuwaiti|Kyrgyz|Lao|Latvian|Lebanese|Liberian|Libyan|Liechtenstein|Lithuanian|Luxembourger|Malagasy|Malawian|Malaysian|Maldivian|Malian|Maltese|Mauritanian|Mauritian|Mexican|Moldovan|Monegasque|Mongolian|Montenegrin|Moroccan|Mozambican|Namibian|Nepalese|New Zealander|Nicaraguan|Nigerian|North Korean|Northern Irish|Norwegian|Omani|Pakistani|Palauan|Palestinian|Panamanian|Papua New Guinean|Paraguayan|Peruvian|Polish|Portuguese|Qatari|Romanian|Russian|Rwandan|Saint Lucian|Salvadoran|Samoan|Saudi Arabian|Scottish|Senegalese|Serbian|Singaporean|Slovak|Slovenian|Somali|South African|South Korean|Spanish|Sri Lankan|Sudanese|Surinamese|Swedish|Swiss|Syrian|Taiwanese|Tajik|Tanzanian|Thai|Togolese|Trinidadian|Tunisian|Turkish|Turkmen|Tuvaluan|Ugandan|Ukrainian|Emirati|Uruguayan|Uzbek|Vatican citizen|Venezuelan|Vietnamese|Welsh|Yemeni|Zambian|Zimbabwean`
  .split('|')
  .sort((first, second) => first.localeCompare(second));
const eeaNationalities = new Set([
  'Austrian', 'Belgian', 'Bulgarian', 'Croatian', 'Cypriot', 'Czech', 'Danish',
  'Dutch', 'Estonian', 'Finnish', 'French', 'German', 'Greek', 'Hungarian',
  'Icelandic', 'Irish', 'Italian', 'Latvian', 'Liechtenstein', 'Lithuanian',
  'Luxembourger', 'Maltese', 'Norwegian', 'Polish', 'Portuguese', 'Romanian',
  'Slovak', 'Slovenian', 'Spanish', 'Swedish'
]);

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

const calculateIrishStampDuty = (price) => (
  Math.round((
    Math.min(price, 1_000_000) * 0.01
    + Math.min(Math.max(price - 1_000_000, 0), 500_000) * 0.02
    + Math.max(price - 1_500_000, 0) * 0.06
  ) * 100) / 100
);

function NationalitySelect({ value, onChange, required, listId }) {
  const [query, setQuery] = useState(value);
  const [open, setOpen] = useState(false);
  const inputRef = useRef(null);
  const listRef = useRef(null);
  const filteredNationalities = nationalities.filter(nationality =>
    nationality.toLocaleLowerCase().includes(query.toLocaleLowerCase())
  );

  useEffect(() => {
    if (inputRef.current) {
      inputRef.current.setCustomValidity(value ? '' : 'Select a nationality from the suggestions.');
    }
  }, [value]);

  const selectNationality = (nationality) => {
    onChange(nationality);
    setQuery(nationality);
    setOpen(false);
    inputRef.current?.setCustomValidity('');
  };

  const handleKeyDown = (event) => {
    if (event.key === 'ArrowDown' && filteredNationalities.length) {
      event.preventDefault();
      setOpen(true);
      listRef.current?.querySelector('button')?.focus();
    } else if (event.key === 'Enter' && open && filteredNationalities.length) {
      event.preventDefault();
      selectNationality(filteredNationalities[0]);
    } else if (event.key === 'Escape') {
      setOpen(false);
      setQuery(value);
    }
  };

  const handleOptionKeyDown = (event, index) => {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      focusOption(index + (event.key === 'ArrowDown' ? 1 : -1));
    } else if (event.key === 'Escape') {
      setOpen(false);
      inputRef.current?.focus();
    }
  };

  return (
    <div
      className="nationality-select"
      onBlur={event => {
        if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false);
      }}
    >
      <input
        ref={inputRef}
        type="text"
        role="combobox"
        aria-autocomplete="list"
        aria-expanded={open}
        aria-controls={listId}
        autoComplete="off"
        placeholder="Search nationalities"
        value={value || query}
        required={required}
        onFocus={() => setOpen(true)}
        onChange={event => {
          setQuery(event.target.value);
          onChange('');
          setOpen(true);
        }}
        onKeyDown={handleKeyDown}
      />
      {open ? (
        <div className="nationality-options" id={listId} role="listbox" ref={listRef}>
          {filteredNationalities.length ? filteredNationalities.map(nationality => (
            <button
              type="button"
              role="option"
              aria-selected={value === nationality}
              key={nationality}
              onMouseDown={event => event.preventDefault()}
              onClick={() => selectNationality(nationality)}
            >
              {nationality}
            </button>
          )) : <span className="nationality-no-results">No matching nationalities</span>}
        </div>
      ) : null}
    </div>
  );
}

function FormSelect({ value, onChange, options, required, listId }) {
  const [open, setOpen] = useState(false);
  const inputRef = useRef(null);
  const listRef = useRef(null);
  const selectedIndex = options.indexOf(value);

  const selectOption = (option) => {
    onChange(option);
    setOpen(false);
  };

  const focusOption = (index) => {
    const optionButtons = listRef.current?.querySelectorAll('button');
    if (!optionButtons?.length) return;
    const wrappedIndex = (index + optionButtons.length) % optionButtons.length;
    optionButtons[wrappedIndex].focus();
  };

  const handleKeyDown = (event) => {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      if (!open) {
        setOpen(true);
        window.requestAnimationFrame(() => {
          focusOption(selectedIndex >= 0
            ? selectedIndex + 1
            : event.key === 'ArrowDown' ? 1 : options.length);
        });
      } else {
        const currentIndex = Array.from(listRef.current?.querySelectorAll('button') || [])
          .indexOf(document.activeElement);
        focusOption(currentIndex + (event.key === 'ArrowDown' ? 1 : -1));
      }
    } else if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      if (!open) setOpen(true);
      else if (selectedIndex >= 0) selectOption(options[selectedIndex]);
    } else if (event.key === 'Escape') {
      setOpen(false);
      inputRef.current?.focus();
    }
  };

  return (
    <div
      className="mortgage-custom-select"
      onBlur={event => {
        if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false);
      }}
    >
      <input
        ref={inputRef}
        className="mortgage-custom-select-input"
        type="text"
        role="combobox"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        aria-required={required}
        readOnly
        required={required}
        value={value}
        placeholder="Select an option"
        onClick={() => setOpen(current => !current)}
        onKeyDown={handleKeyDown}
      />
      {open ? (
        <div className="mortgage-custom-select-options" id={listId} role="listbox" ref={listRef}>
          <button
            type="button"
            role="option"
            aria-selected={!value}
            className="mortgage-custom-select-placeholder"
            onClick={() => {
              onChange('');
              setOpen(false);
              inputRef.current?.focus();
            }}
            onKeyDown={event => handleOptionKeyDown(event, 0)}
          >
            Select an option
          </button>
          <div>
            {options.map((option, index) => (
              <button
                type="button"
                role="option"
                aria-selected={value === option}
                key={option}
                onClick={() => {
                  selectOption(option);
                  inputRef.current?.focus();
                }}
                onKeyDown={event => {
                  handleOptionKeyDown(event, index + 1);
                }}
              >
                {option}
              </button>
            ))}
          </div>
        </div>
      ) : null}
    </div>
  );
}

export default function MortgageApplication({ onCancel, onSubmit, submitting, error }) {
  const [form, setForm] = useState(createInitialForm);
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
    if (path[0] === 'applicants' && path[2] === 'nationality') {
      const nonEea = value ? (eeaNationalities.has(value) ? 'No' : 'Yes') : '';
      updated = updatePath(updated, ['applicants', path[1], 'nonEea'], nonEea);
      if (nonEea !== 'Yes') {
        updated = updatePath(updated, ['applicants', path[1], 'visaType'], '');
        updated = updatePath(updated, ['applicants', path[1], 'visaExpiry'], '');
      }
    }
    if (path.length === 1 && path[0] === 'applicationType') {
      updated = updatePath(updated, ['stampDuty'], value === 'Purchase' && updated.purchasePrice !== ''
        ? calculateIrishStampDuty(Number(updated.purchasePrice))
        : 0);
    }
    if (path.length === 1 && path[0] === 'purchasePrice') {
      updated = updatePath(updated, ['stampDuty'], updated.applicationType === 'Purchase' && value !== ''
        ? calculateIrishStampDuty(Number(value))
        : 0);
    }
    return updated;
  });
  const read = (path) => readPath(form, path);
  const visibleStepIndices = form.propertyFound === 'No'
    ? steps.map((_, index) => index).filter(index => index !== 4)
    : steps.map((_, index) => index);
  const currentStepPosition = visibleStepIndices.indexOf(step);

  const renderField = (label, path, options = {}) => {
    const value = read(path) ?? '';
    const required = Boolean(options.required);
    const choices = options.options || (options.type === 'yesNo' ? selectOptions.yesNo : null);
    return (
      <label className={fieldClass} key={path.join('.')}>
        <span>
          {label}{options.unit ? ` (${options.unit})` : ''}
          {required ? <span className="mortgage-required-marker"> *</span> : null}
        </span>
        {options.nationality ? (
          <NationalitySelect
            value={value}
            onChange={selected => update(path, selected)}
            required={required}
            listId={`nationality-options-${path.join('-')}`}
          />
        ) : choices ? (
          <FormSelect
            value={value}
            onChange={selected => update(path, selected)}
            options={choices}
            required={required}
            listId={`select-options-${path.join('-')}`}
          />
        ) : (
          <input
            type={options.type || 'text'}
            value={value}
            onChange={event => update(path, event.target.value)}
            required={required}
            min={options.min}
            max={options.max}
            step={options.step}
            maxLength={options.maxLength}
            autoComplete={options.autoComplete}
            readOnly={options.readOnly}
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
      <span>{label}{required ? <span className="mortgage-required-marker"> *</span> : null}</span>
    </label>
  );

  const renderRepeatGroup = (title, path, fields, {
    itemLabel = title,
    addLabel = `Add ${title.toLowerCase()}`,
    currentKey,
    currentLabel = 'Current address',
    allowEmpty = false
  } = {}) => {
    const items = read(path) || [];
    return (
      <div className="mortgage-repeat-group" key={path.join('.')}>
        <h3>{title}</h3>
        {items.map((item, index) => (
          <fieldset className="mortgage-repeat-item" key={`${path.join('.')}-${index}`}>
            <legend>{itemLabel} {index + 1}.</legend>
            <div className="mortgage-form-grid">
              {fields.map(field => {
                const options = field.options || {};
                const required = options.requiredWhen
                  ? options.requiredWhen(item)
                  : options.required;
                return renderField(field.label, [...path, index, field.key], { ...options, required });
              })}
            </div>
            {currentKey ? (
              <label className="mortgage-check">
                <input
                  type="checkbox"
                  checked={Boolean(item[currentKey])}
                  onChange={event => {
                    const updatedItem = { ...item, [currentKey]: event.target.checked };
                    if (event.target.checked) updatedItem.to = '';
                    update(path, items.map((existing, itemIndex) => itemIndex === index ? updatedItem : existing));
                  }}
                />
                <span>{currentLabel}</span>
              </label>
            ) : null}
            {items.length > 1 || allowEmpty ? (
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
          onClick={() => update(path, [
            ...items,
            {
              ...Object.fromEntries(fields.map(field => [field.key, ''])),
              ...(currentKey ? { [currentKey]: false } : {})
            }
          ])}
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

  const nextStep = () => {
    if (!formRef.current?.reportValidity()) return;
    const nextStepIndex = visibleStepIndices[currentStepPosition + 1];
    if (nextStepIndex !== undefined) setStep(nextStepIndex);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const previousStep = () => {
    const previousStepIndex = visibleStepIndices[currentStepPosition - 1];
    if (previousStepIndex !== undefined) setStep(previousStepIndex);
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
            <span>You can continue with an Agreement in Principle, using an estimated target price and deposit for the costs section. Add property details later.</span>
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
                  {renderField('Gender', [...applicantPath, 'gender'], { options: selectOptions.gender })}
                  {renderField('PPSN', [...applicantPath, 'ppsn'], {required: true, help: 'Your PPS number is stored with this application.' })}
                  {renderField('Marital / civil status', [...applicantPath, 'maritalStatus'], { options: selectOptions.maritalStatus, required: true })}
                  {renderField('Nationality', [...applicantPath, 'nationality'], { nationality: true, required: true })}
                  {renderField('Residency status', [...applicantPath, 'residency'], { options: selectOptions.residency, required: true })}
                  {renderField('Years living in Ireland', [...applicantPath, 'yearsInIreland'], { type: 'number', min: 0, step: 1, required: true })}
                  {applicant.nationality && !eeaNationalities.has(applicant.nationality)
                    ? renderField('Non-EEA national?', [...applicantPath, 'nonEea'], { type: 'yesNo', required: true })
                    : null}
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
                  { key: 'eircode', label: 'Eircode', options: { required: true } },
                  { key: 'from', label: 'From', options: { type: 'date', required: true } },
                  { key: 'to', label: 'To', options: { type: 'date', requiredWhen: item => !item.current } }
                ], {
                  itemLabel: 'Address',
                  addLabel: 'Add address history',
                  currentKey: 'current'
                })}
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
                      {renderField('Tenure with current employer', [...path, 'yearsWithEmployer'], { type: 'number', min: 0, step: '0.1', unit: 'years', required: true })}
                      {applicant.yearsWithEmployer !== '' && Number(applicant.yearsWithEmployer) < 2
                        ? renderField('Previous employer details', [...path, 'previousEmployer'], {
                          required: true,
                          help: 'Include the employer name, role and dates.'
                        })
                        : null}
                    </>
                  ) : null}
                  {renderField('Gross basic annual salary', [...path, 'grossAnnualSalary'], { type: 'number', min: 0, step: '0.01', unit: 'EUR', required: true })}
                  {renderField('Net monthly pay', [...path, 'netMonthlyPay'], { type: 'number', min: 0, step: '0.01', unit: 'EUR', required: true })}
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
                  { key: 'lender', label: 'Lender', options: { required: true } },
                  { key: 'balance', label: 'Outstanding balance', options: { type: 'number', min: 0, step: '0.01', unit: 'EUR', required: true } },
                  { key: 'repayment', label: 'Monthly repayment', options: { type: 'number', min: 0, step: '0.01', unit: 'EUR', required: true } },
                  { key: 'monthsRemaining', label: 'Months remaining', options: { type: 'number', min: 0, step: 1, required: true } },
                  { key: 'refinance', label: 'To be refinanced?', options: { type: 'yesNo', required: true } }
                ], { itemLabel: 'Loan', addLabel: 'Add loan', allowEmpty: true })}
                {renderRepeatGroup('Credit cards', [...path, 'creditCards'], [
                  { key: 'provider', label: 'Provider', options: { required: true } },
                  { key: 'limit', label: 'Credit limit', options: { type: 'number', min: 0, step: '0.01', unit: 'EUR', required: true } },
                  { key: 'balance', label: 'Current balance', options: { type: 'number', min: 0, step: '0.01', unit: 'EUR', required: true } }
                ], { itemLabel: 'Credit card', addLabel: 'Add credit card', allowEmpty: true })}
                <div className="mortgage-form-grid">
                  {renderField('Maintenance and childcare costs', [...path, 'maintenanceAndChildcare'], { type: 'number', min: 0, step: '0.01', unit: 'EUR / month', required: true })}
                </div>
              </fieldset>
            );
          })}
          {renderRepeatGroup('Existing properties and mortgages', ['existingProperties'], [
            { key: 'address', label: 'Property address', options: { required: true } },
            { key: 'mortgageBalance', label: 'Mortgage balance', options: { type: 'number', min: 0, step: '0.01', unit: 'EUR', required: true } },
            { key: 'monthlyRepayment', label: 'Monthly repayment', options: { type: 'number', min: 0, step: '0.01', unit: 'EUR', required: true } },
            { key: 'forbearance', label: 'Forbearance arrangements?', options: { type: 'yesNo', required: true } }
          ], { itemLabel: 'Existing property', addLabel: 'Add existing property', allowEmpty: true })}
          <div className="mortgage-form-grid">
            {renderField('Insolvency or judgements?', ['insolvency'], { type: 'yesNo', required: true })}
            {form.insolvency === 'Yes' ? renderField('Please provide details', ['insolvencyDetails'], { required: true }) : null}
            {renderField('Expected future changes to finances?', ['futureChanges'], { type: 'yesNo', required: true })}
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
              {renderField('BER rating', ['ber'], { required: form.propertyStatus === 'Second-hand' })}
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
          <p className="mortgage-step-intro">
            {form.propertyFound === 'No'
              ? 'For an Approval in Principle, enter your estimated target price and deposit. You can update these when you find a property.'
              : 'Enter the purchase costs and explain where the deposit and other funds are coming from.'}
          </p>
          <div className="mortgage-form-grid">
            {renderField(
              form.propertyFound === 'No' ? 'Estimated purchase price / target budget' : 'Purchase price / current value',
              ['purchasePrice'],
              { type: 'number', min: 0, step: '0.01', unit: 'EUR', required: true }
            )}
            {renderField('Stamp duty (calculated)', ['stampDuty'], {
              type: 'number',
              min: 0,
              step: '0.01',
              unit: 'EUR',
              readOnly: true,
              help: form.applicationType === 'Purchase'
                ? 'Calculated using Irish residential rates: 1% up to €1m, 2% from €1m to €1.5m, and 6% above €1.5m.'
                : 'No stamp duty is calculated for this application type.'
            })}
            {renderField('Legal costs', ['legalCosts'], { type: 'number', min: 0, step: '0.01', unit: 'EUR' })}
            {renderField('Renovation costs', ['renovationCosts'], { type: 'number', min: 0, step: '0.01', unit: 'EUR' })}
          </div>
          {renderRepeatGroup('Sources of funds', ['funds'], [
            { key: 'source', label: 'Source (savings, gift, inheritance, sale proceeds, Help to Buy)', options: { required: true } },
            { key: 'amount', label: 'Amount', options: { type: 'number', min: 0.01, step: '0.01', unit: 'EUR', required: true } },
            {
              key: 'donorRelationship',
              label: 'Donor relationship (if gift)',
              options: { requiredWhen: item => (item.source || '').toLowerCase().includes('gift') }
            }
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
        <p className="mortgage-step-intro">Supporting document uploads are not available yet. You can submit your application now and provide documents later.</p>
        <div className="mortgage-documents">
          {[
            'Photo ID and proof of address',
            'PPSN proof',
            'Payslips',
            'Six months of account statements',
            'Salary certificate and Employment Detail Summary',
            'Conditional documents (Forms 11, gift letter, IRP, separation agreement)'
          ].map(label => (
            <div className="mortgage-upload-placeholder" key={label}>
              <span>{label}</span>
              <small>Document upload will be available later.</small>
            </div>
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
    onSubmit(form);
  };

  return (
    <section className="mortgage-application">
      <div className="mortgage-title-row">
        <div>
          <h2 className="page-heading">Mortgage application</h2>
          <p>Complete each section to send your application for review.</p>
        </div>
      </div>
      <div className="mortgage-progress" aria-label={`Step ${currentStepPosition + 1} of ${visibleStepIndices.length}`}>
        <div className="mortgage-progress-copy">
          <span>Step {currentStepPosition + 1} of {visibleStepIndices.length}</span>
          <strong>{steps[step].title}</strong>
          <span>{Math.round(((currentStepPosition + 1) / visibleStepIndices.length) * 100)}% complete</span>
        </div>
        <div
          className="mortgage-progress-track"
          role="progressbar"
          aria-label="Application progress"
          aria-valuemin="1"
          aria-valuemax={visibleStepIndices.length}
          aria-valuenow={currentStepPosition + 1}
          aria-valuetext={`Step ${currentStepPosition + 1} of ${visibleStepIndices.length}: ${steps[step].title}`}
        >
          <div className="mortgage-progress-fill" style={{ width: `${((currentStepPosition + 1) / visibleStepIndices.length) * 100}%` }} />
        </div>
        <ol className="mortgage-step-list">
          {visibleStepIndices.map((stepIndex, position) => (
            <li key={steps[stepIndex].title} className={position === currentStepPosition ? 'is-current' : position < currentStepPosition ? 'is-complete' : ''}>
              <span className="mortgage-step-number">{position < currentStepPosition ? '✓' : position + 1}</span>
              <span className="mortgage-step-name">{steps[stepIndex].title}</span>
            </li>
          ))}
        </ol>
      </div>
      <form ref={formRef} onSubmit={handleSubmit} className="mortgage-form" noValidate={false}>
        <div className="mortgage-section-heading">
          <span>SECTION {currentStepPosition + 1}</span>
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
          {currentStepPosition < visibleStepIndices.length - 1 ? (
            <button type="button" className="primary-button" onClick={nextStep}>Continue</button>
          ) : (
            <button type="submit" className="primary-button" disabled={submitting}>
              {submitting ? 'Submitting application...' : 'Submit application'}
            </button>
          )}
        </div>
        <p className="mortgage-required-note"><span className="mortgage-required-marker">*</span> Required fields</p>
      </form>
    </section>
  );
}
