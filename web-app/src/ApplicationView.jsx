import React, { useEffect, useState } from 'react';
import { deleteDoc, doc, onSnapshot, serverTimestamp, updateDoc } from 'firebase/firestore';
import { db } from './firebase';
import './ApplicationView.css';

const aiApiUrl = (import.meta.env.VITE_AI_API_URL || 'http://127.0.0.1:8000').replace(/\/$/, '');

export const getApplicationStatusClassName = (status) => {
  const normalizedStatus = String(status || 'Applied').trim().toLowerCase();
  if (normalizedStatus === 'applied') return 'is-applied';
  if (normalizedStatus === 'approved') return 'is-approved';
  if (normalizedStatus === 'denied' || normalizedStatus === 'declined') return 'is-declined';
  return '';
};

const agentHighlights = {
  'Affordability Agent': [
    ['Gross annual income', 'gross_yearly_income'],
    ['Monthly repayment', 'monthly_repayment'],
    ['Stressed repayment', 'monthly_repayment_stressed'],
    ['DTI', 'dti'],
    ['LTI', 'lti'],
    ['Monthly disposable income', 'disposable_income_monthly'],
    ['Estimated borrowing capacity', 'borrowing_capacity_estimate']
  ],
  'Valuation Agent': [
    ['Estimated property value', 'estimated_value'],
    ['Asking price', 'asking_price'],
    ['Valuation difference', 'valuation_difference_pct'],
    ['LTV', 'ltv'],
    ['Overvaluation warning', 'overvaluation_warning']
  ],
  'Market and Macro Researcher Agent': [],
  'Underwriting Strategist Agent': [
    ['Committee recommendation', 'consensus.recommendation'],
    ['Committee confidence', 'consensus.confidence'],
    ['Proposed APR', 'pricing.apr_pct']
  ]
};

const getValue = (value, path) => path.split('.').reduce(
  (current, key) => current && current[key],
  value
);

const labelFor = (key) => key
  .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
  .replace(/[_-]+/g, ' ')
  .replace(/\b\w/g, character => character.toUpperCase());

const formatValue = (key, value) => {
  if (value === null || value === undefined || value === '') return 'Not provided';
  if (typeof value === 'boolean') return value ? 'Yes' : 'No';
  if (typeof value === 'number') {
    if (key === 'apr_pct') return `${value.toFixed(2)}%`;
    if (key === 'dti' || key === 'ltv' || key.endsWith('_pct')) return `${(value * 100).toFixed(1)}%`;
    if (key === 'confidence') return `${(value * 100).toFixed(0)}%`;
    if (key === 'lti') return `${value.toFixed(2)}x`;
    if (key.endsWith('_bps')) return `${value} bps`;
    if (/income|repayment|capacity|price|value|amount|deposit/i.test(key)) {
      return new Intl.NumberFormat('en-IE', {
        style: 'currency',
        currency: 'EUR',
        maximumFractionDigits: 0
      }).format(value);
    }
  }
  return String(value);
};

function renderSubmittedData(value) {
  if (Array.isArray(value)) {
    return value.length ? (
      <ul className="submitted-data-array">
        {value.map((item, index) => <li key={index}>{renderSubmittedData(item)}</li>)}
      </ul>
    ) : <span>None provided</span>;
  }

  if (value && typeof value === 'object') {
    const entries = Object.entries(value).filter(([, entry]) => entry !== null && entry !== '');
    if (!entries.length) return <span>None provided</span>;
    return (
      <dl className="submitted-data-list">
        {entries.map(([key, entry]) => (
          <div key={key}>
            <dt>{labelFor(key)}</dt>
            <dd>{renderSubmittedData(entry)}</dd>
          </div>
        ))}
      </dl>
    );
  }

  if (typeof value === 'boolean') return value ? 'Yes' : 'No';
  return String(value);
}

function AgentCard({ agent }) {
  const highlights = agentHighlights[agent.agent] || [];
  const strategist = agent.agent === 'Underwriting Strategist Agent';
  const votes = strategist ? agent.result?.votes || [] : [];

  return (
    <article className="agent-result-card">
      <div className="agent-result-heading">
        <h3>{agent.agent}</h3>
        <span className={`agent-status status-${String(agent.status || '').toLowerCase()}`}>{agent.status}</span>
      </div>
      <p className="agent-reasoning">{agent.reasoning || 'No written summary was provided.'}</p>

      {agent.agent === 'Market and Macro Researcher Agent' && agent.result?.bullets?.length ? (
        <ul className="agent-bullet-list">
          {agent.result.bullets.map((bullet, index) => <li key={index}>{bullet}</li>)}
        </ul>
      ) : null}

      {highlights.some(([, path]) => getValue(agent.result, path) !== undefined) ? (
        <dl className="agent-highlights">
          {highlights.map(([label, path]) => {
            const value = getValue(agent.result, path);
            return value === undefined ? null : (
              <div key={path}>
                <dt>{label}</dt>
                <dd>{formatValue(path.split('.').pop(), value)}</dd>
              </div>
            );
          })}
        </dl>
      ) : null}

      {agent.warnings?.length ? (
        <div className="agent-warning-block">
          <strong>Items to review</strong>
          <ul>{agent.warnings.map((warning, index) => <li key={index}>{warning}</li>)}</ul>
        </div>
      ) : null}

      {strategist && votes.length ? (
        <details className="agent-votes">
          <summary>View committee votes ({votes.length})</summary>
          {votes.map((vote, index) => (
            <div className="agent-vote" key={`${vote.model}-${index}`}>
              <strong>{vote.model}</strong>
              <span>{vote.recommendation || vote.status}</span>
              {vote.key_reasons?.length ? <p>{vote.key_reasons.join(' · ')}</p> : null}
              {vote.conditions?.length ? <p>Conditions: {vote.conditions.join(' · ')}</p> : null}
            </div>
          ))}
        </details>
      ) : null}

      <details className="agent-technical-details">
        <summary>Inputs and calculation details</summary>
        <pre>{JSON.stringify({ inputs_used: agent.inputs_used, result: agent.result }, null, 2)}</pre>
      </details>
    </article>
  );
}

export default function ApplicationView({
  application: initialApplication,
  user,
  onBack,
  onDeleted,
  onDecisionComplete
}) {
  const [application, setApplication] = useState(initialApplication);
  const [assessment, setAssessment] = useState(null);
  const [assessmentError, setAssessmentError] = useState('');
  const [analysisBusy, setAnalysisBusy] = useState(false);
  const [analysisError, setAnalysisError] = useState('');
  const [decisionBusy, setDecisionBusy] = useState(false);
  const [actionError, setActionError] = useState('');
  const [editMode, setEditMode] = useState(false);
  const [editBusy, setEditBusy] = useState(false);
  const [editFields, setEditFields] = useState({});
  const [deleteBusy, setDeleteBusy] = useState(false);

  useEffect(() => {
    setApplication(initialApplication);
  }, [initialApplication]);

  useEffect(() => onSnapshot(
    doc(db, 'applications', initialApplication.id),
    snapshot => {
      if (snapshot.exists()) setApplication({ id: snapshot.id, ...snapshot.data() });
    },
    error => setActionError(`Could not refresh application: ${error.message}`)
  ), [initialApplication.id]);

  const hasConsent = application.consent?.aiAnalysis === true;
  const assessmentId = application.aiAssessment?.assessmentId;
  useEffect(() => {
    setAssessment(null);
    setAssessmentError('');
    if (!hasConsent || !assessmentId) return undefined;

    return onSnapshot(
      doc(db, 'applications', initialApplication.id, 'aiAssessments', assessmentId),
      snapshot => setAssessment(snapshot.exists() ? snapshot.data() : null),
      error => setAssessmentError(error.message)
    );
  }, [assessmentId, hasConsent, initialApplication.id]);

  const beginEditing = () => {
    setEditFields({
      borrower: application.borrower || '',
      applicantEmail: application.applicantEmail || '',
      phone: application.phone || '',
      location: application.location || '',
      loanRequested: application.loanRequested || ''
    });
    setActionError('');
    setEditMode(true);
  };

  const saveChanges = async (event) => {
    event.preventDefault();
    setEditBusy(true);
    setActionError('');
    try {
      await updateDoc(doc(db, 'applications', application.id), {
        borrower: editFields.borrower.trim(),
        applicantEmail: editFields.applicantEmail.trim(),
        phone: editFields.phone.trim(),
        location: editFields.location.trim(),
        loanRequested: editFields.loanRequested
      });
      setEditMode(false);
    } catch (error) {
      setActionError(`Could not update application: ${error.message}`);
    } finally {
      setEditBusy(false);
    }
  };

  const startAnalysis = async () => {
    setAnalysisBusy(true);
    setAnalysisError('');
    try {
      const token = await user.getIdToken();
      let response;
      try {
        response = await fetch(
          `${aiApiUrl}/applications/${encodeURIComponent(application.id)}/analysis`,
          { method: 'POST', headers: { Authorization: `Bearer ${token}` } }
        );
      } catch (error) {
        if (error instanceof TypeError) {
          throw new Error(
            `Could not reach the AI API at ${aiApiUrl}. Start it from the ai-engine folder with its virtual environment activated, and make sure WEB_APP_ORIGINS includes this web app's origin.`
          );
        }
        throw error;
      }
      const payload = await response.json();
      if (!response.ok) throw new Error(payload.detail || 'The AI service could not start the analysis.');
    } catch (error) {
      setAnalysisError(error.message);
    } finally {
      setAnalysisBusy(false);
    }
  };

  const recordDecision = async (decision) => {
    const approved = decision === 'approve';
    const borrower = application.borrower || 'this applicant';
    const confirmation = approved
      ? `Approve the application for ${borrower}? This records the decision but does not mint a ledger note.`
      : `Deny the application for ${borrower}?`;
    if (!window.confirm(confirmation)) return;

    setDecisionBusy(true);
    setActionError('');
    try {
      await updateDoc(doc(db, 'applications', application.id), {
        status: approved ? 'Approved' : 'Declined',
        underwriterDecision: {
          decision: approved ? 'approved' : 'declined',
          decidedBy: user.uid,
          decidedAt: serverTimestamp()
        }
      });
      onDecisionComplete();
    } catch (error) {
      setActionError(`Could not save the underwriter decision: ${error.message}`);
    } finally {
      setDecisionBusy(false);
    }
  };

  const deleteApplication = async () => {
    if (!window.confirm(`Delete the application for ${application.borrower || 'this borrower'}? This cannot be undone.`)) return;
    setDeleteBusy(true);
    setActionError('');
    try {
      await deleteDoc(doc(db, 'applications', application.id));
      onDeleted();
    } catch (error) {
      setActionError(`Could not delete application: ${error.message}`);
      setDeleteBusy(false);
    }
  };

  const aiStatus = application.aiAssessment?.status;
  const analysisRunning = analysisBusy || aiStatus === 'running';
  const analysisComplete = aiStatus === 'assessed';
  const analysisButtonLabel = analysisRunning
    ? 'AI analysis running...'
    : analysisComplete
      ? 'AI Analysis Complete'
      : aiStatus === 'error'
        ? 'Retry AI Analysis'
        : 'Start AI Analysis';

  return (
    <section className="application-view">
      <button type="button" className="application-view-back" onClick={onBack}>← Back to applications</button>
      <header className="application-view-heading">
        <div>
          <p className="application-view-eyebrow">Mortgage application</p>
          <h1>{application.borrower || 'Unnamed borrower'}</h1>
          <p className="application-view-subtitle">
            {application.applicantEmail || 'No email'} · Application ID: {application.id}
          </p>
        </div>
        <span className={`application-view-status ${getApplicationStatusClassName(application.status)}`}>
          {application.status || 'Applied'}
        </span>
      </header>

      <section className="application-view-panel">
        <div className="application-view-section-heading application-details-heading">
          <h2>Application details</h2>
          <p className="application-submitted-at">
            Submitted {application.timestamp?.toDate ? application.timestamp.toDate().toLocaleString() : 'date unavailable'}
          </p>
        </div>

        {editMode ? (
          <form onSubmit={saveChanges} className="application-edit-form">
            {[
              ['borrower', 'Borrower name', 'text'],
              ['applicantEmail', 'Email', 'email'],
              ['phone', 'Phone', 'tel'],
              ['location', 'Location', 'text'],
              ['loanRequested', 'Loan requested (€)', 'number']
            ].map(([key, label, type]) => (
              <label key={key}>{label}
                <input
                  type={type}
                  min={type === 'number' ? '1' : undefined}
                  required
                  value={editFields[key] || ''}
                  onChange={event => setEditFields({ ...editFields, [key]: event.target.value })}
                />
              </label>
            ))}
            <div className="application-view-actions">
              <button type="submit" className="primary-button" disabled={editBusy}>{editBusy ? 'Saving...' : 'Save changes'}</button>
              <button type="button" className="secondary-button" disabled={editBusy} onClick={() => setEditMode(false)}>Cancel</button>
            </div>
          </form>
        ) : (
          <dl className="application-view-details">
            <div><dt>Borrower</dt><dd>{application.borrower || 'Not provided'}</dd></div>
            <div><dt>Email</dt><dd>{application.applicantEmail || 'Not provided'}</dd></div>
            <div><dt>Phone</dt><dd>{application.phone || 'Not provided'}</dd></div>
            <div><dt>Location</dt><dd>{application.location || 'Not provided'}</dd></div>
            <div><dt>Loan requested</dt><dd>{formatValue(
              'loanRequested',
              Number.isFinite(Number(application.loanRequested)) ? Number(application.loanRequested) : application.loanRequested
            )}</dd></div>
            <div><dt>Application type</dt><dd>{application.applicationData?.applicationType || 'Not provided'}</dd></div>
            <div><dt>AI analysis consent</dt><dd>{hasConsent ? 'Given' : 'Not given'}</dd></div>
          </dl>
        )}

        {application.applicationData ? (
          <details className="application-submitted-data">
            <summary>View full submitted application</summary>
            {renderSubmittedData(application.applicationData)}
          </details>
        ) : null}
        <div className="application-view-footer-actions">
          {!editMode ? <button type="button" className="secondary-button" onClick={beginEditing}>Edit details</button> : <span />}
          <div className="application-view-delete">
            <button type="button" className="delete-application-button" onClick={deleteApplication} disabled={deleteBusy}>
              {deleteBusy ? 'Deleting...' : 'Delete application'}
            </button>
          </div>
        </div>
      </section>

      {hasConsent ? (
      <section className="application-view-panel ai-assessment-panel">
        <div className="application-view-section-heading">
          <div>
            <h2>AI analysis</h2>
            <p>Decision support only. The underwriter makes the final lending decision.</p>
          </div>
          <button
            type="button"
            className="primary-button start-analysis-button"
            onClick={startAnalysis}
            disabled={!hasConsent || analysisRunning || analysisComplete}
          >
            {analysisButtonLabel}
          </button>
        </div>
        {analysisRunning ? <p className="application-view-notice" role="status">Analysis is running. This may take a few minutes.</p> : null}
        {analysisError ? <p className="application-view-error" role="alert">{analysisError}</p> : null}
        {assessmentError ? <p className="application-view-error" role="alert">Could not load agent results: {assessmentError}</p> : null}
        {aiStatus === 'error' ? (
          <p className="application-view-error" role="alert">
            The last analysis failed: {application.aiAssessment.reason || 'No error details were saved.'}
          </p>
        ) : null}
        {aiStatus === 'not_assessed' ? (
          <p className="application-view-notice">{application.aiAssessment.reason || 'The application was not assessed.'}</p>
        ) : null}

        {application.aiAssessment?.status === 'assessed' ? (
          <>
            <dl className="assessment-summary">
              {[
                ['Recommendation', application.aiAssessment.recommendation],
                ['Risk rating', application.aiAssessment.riskRating],
                ['DTI', application.aiAssessment.dti],
                ['LTI', application.aiAssessment.lti],
                ['LTV', application.aiAssessment.ltv],
                ['Proposed APR', application.aiAssessment.aprBasisPoints == null ? null : `${(application.aiAssessment.aprBasisPoints / 100).toFixed(2)}%`]
              ].map(([label, value]) => value == null ? null : (
                <div key={label}><dt>{label}</dt><dd>{value}</dd></div>
              ))}
            </dl>
            {assessment ? (
              <div className="agent-results">
                {assessment.agents?.map(agent => <AgentCard key={agent.agent} agent={agent} />)}
              </div>
            ) : assessmentError ? null : assessmentId
              ? <p className="application-view-notice">Loading detailed agent results...</p>
              : <p className="application-view-notice">The assessment summary is available, but this record has no detailed agent report.</p>}
            <p className="assessment-disclaimer">{application.aiAssessment.disclaimer}</p>
          </>
        ) : null}
        {!aiStatus ? <p className="application-view-notice">No AI assessment has been run for this application yet.</p> : null}
      </section>
      ) : (
        <section className="application-view-panel no-consent-panel">
          <p>AI analysis unavailable because the applicant has not given consent.</p>
        </section>
      )}

      <section className="application-view-panel decision-panel">
        <div>
          <h2>Approve Mortgage Application?</h2>
        </div>
        <div className="application-view-actions">
          <button type="button" className="approve-button" onClick={() => recordDecision('approve')} disabled={decisionBusy}>
            Approve
          </button>
          <button type="button" className="deny-button" onClick={() => recordDecision('deny')} disabled={decisionBusy}>
            Deny
          </button>
        </div>
      </section>

      {actionError ? <p className="application-view-error" role="alert">{actionError}</p> : null}
    </section>
  );
}
