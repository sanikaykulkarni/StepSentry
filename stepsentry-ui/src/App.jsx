import { useEffect, useState } from 'react';

import 'bootstrap/dist/css/bootstrap.min.css';

import 'bootstrap-icons/font/bootstrap-icons.css';

import './App.css';

const API_URL = 'http://localhost:8080/api/workflows';

const emptyRequirement = {

  title: '',

  description: '',

  mandatory: true,

  sortOrder: 0

};

const emptyEvidence = {

  type: 'DOCUMENT',

  title: '',

  evidenceUrl: '',

  description: '',

  sourceVersion: ''

};

function App() {

  const [workflows, setWorkflows] = useState([]);

  const [selectedWorkflowId, setSelectedWorkflowId] = useState('');

  const [requirements, setRequirements] = useState([]);

  const [evidenceByRequirement, setEvidenceByRequirement] = useState({});

  const [filter, setFilter] = useState('All');

  const [page, setPage] = useState('Overview');

  const [loading, setLoading] = useState(true);

  const [detailsLoading, setDetailsLoading] = useState(false);

  const [error, setError] = useState('');

  const [actionError, setActionError] = useState('');

  const [success, setSuccess] = useState('');

  const [readiness, setReadiness] = useState(null);

  const [readinessByWorkflow, setReadinessByWorkflow] = useState({});

  const [readinessLoading, setReadinessLoading] = useState(false);

  const [readinessError, setReadinessError] = useState('');

  const [showWorkflowForm, setShowWorkflowForm] = useState(false);

  const [showRequirementForm, setShowRequirementForm] = useState(false);

  const [evidenceFormFor, setEvidenceFormFor] = useState(null);

  const [saving, setSaving] = useState(false);

  const [workflowForm, setWorkflowForm] = useState({

    name: '',

    description: '',

    currentVersion: ''

  });

  const [requirementForm, setRequirementForm] = useState(emptyRequirement);

  const [evidenceForm, setEvidenceForm] = useState(emptyEvidence);

  const selectedWorkflow = workflows.find(

    workflow => String(workflow.id) === String(selectedWorkflowId)

  );

  async function apiRequest(url, options = {}) {

    const response = await fetch(url, {

      ...options,

      headers: {

        ...(options.body ? { 'Content-Type': 'application/json' } : {}),

        ...options.headers

      }

    });

    if (!response.ok) {

      const message = await response.text();

      throw new Error(message || `Request failed (${response.status})`);

    }

    if (response.status === 204) return null;

    const text = await response.text();

    return text ? JSON.parse(text) : null;

  }

  async function loadWorkflows() {

    setLoading(true);

    setError('');

    try {

      const data = await apiRequest(API_URL);

      setWorkflows(data);

      setSelectedWorkflowId(current => {

        if (data.some(item => String(item.id) === String(current))) {

          return current;

        }

        return data.length ? String(data[0].id) : '';

      });

    } catch (err) {

      setError(`Could not load workflows: ${err.message}`);

    } finally {

      setLoading(false);

    }

  }

  async function loadWorkflowDetails(workflowId) {

    if (!workflowId) {

      setRequirements([]);

      setEvidenceByRequirement({});

      return;

    }

    setDetailsLoading(true);

    setActionError('');

    try {

      const requirementData = await apiRequest(

        `${API_URL}/${workflowId}/requirements`

      );

      setRequirements(requirementData);

      const evidenceResults = await Promise.all(

        requirementData.map(async requirement => {

          try {

            const evidence = await apiRequest(

              `${API_URL}/${workflowId}/requirements/${requirement.id}/evidence`

            );

            return [requirement.id, evidence];

          } catch {

            return [requirement.id, []];

          }

        })

      );

      setEvidenceByRequirement(Object.fromEntries(evidenceResults));

    } catch (err) {

      setRequirements([]);

      setEvidenceByRequirement({});

      setActionError(`Could not load requirements: ${err.message}`);

    } finally {

      setDetailsLoading(false);

    }

  }

  async function loadReadiness(workflowId = selectedWorkflowId) {

    if (!workflowId) {

      setReadiness(null);

      setReadinessError('');

      return;

    }

    setReadinessLoading(true);

    setReadinessError('');

    try {

      const data = await apiRequest(`${API_URL}/${workflowId}/readiness`);

      setReadiness(data);

    } catch (err) {

      setReadiness(null);

      setReadinessError(`Could not load readiness: ${err.message}`);

    } finally {

      setReadinessLoading(false);

    }

  }

  useEffect(() => {

    // Defer the initial load so state updates do not run synchronously

    // while React is processing this effect.

    const timerId = window.setTimeout(() => {

      void loadWorkflows();

    }, 0);

    return () => window.clearTimeout(timerId);

  }, []);

  useEffect(() => {

    // Defer loading details/readiness to avoid synchronous effect updates.

    const timerId = window.setTimeout(() => {

      if (selectedWorkflowId) {

        void loadWorkflowDetails(selectedWorkflowId);

        void loadReadiness(selectedWorkflowId);

      } else {

        setRequirements([]);

        setEvidenceByRequirement({});

        setReadiness(null);

      }

    }, 0);

    return () => window.clearTimeout(timerId);

  }, [selectedWorkflowId]);

  // Fetch official readiness results for the Overview dashboard.

  useEffect(() => {

    let cancelled = false;

    async function loadAllReadiness() {

      if (workflows.length === 0) {

        setReadinessByWorkflow({});

        return;

      }

      const results = await Promise.all(

        workflows.map(async workflow => {

          try {

            const result = await apiRequest(`${API_URL}/${workflow.id}/readiness`);

            return [workflow.id, result];

          } catch {

            return [workflow.id, null];

          }

        })

      );

      if (!cancelled) {

        setReadinessByWorkflow(Object.fromEntries(results));

      }

    }

    void loadAllReadiness();

    return () => {

      cancelled = true;

    };

  }, [workflows]);

  function navigate(nextPage) {

    setPage(nextPage);

    setActionError('');

    setSuccess('');

    setShowWorkflowForm(false);

    setShowRequirementForm(false);

    setEvidenceFormFor(null);

  }

  async function createWorkflow(event) {

    event.preventDefault();

    setSaving(true);

    setActionError('');

    setSuccess('');

    try {

      const created = await apiRequest(API_URL, {

        method: 'POST',

        body: JSON.stringify({

          name: workflowForm.name.trim(),

          description: workflowForm.description.trim() || null,

          currentVersion: workflowForm.currentVersion.trim() || null

        })

      });

      setWorkflowForm({

        name: '',

        description: '',

        currentVersion: ''

      });

      setShowWorkflowForm(false);

      const data = await apiRequest(API_URL);

      setWorkflows(data);

      setSelectedWorkflowId(String(created.id));

      setPage('Workflows');

      setSuccess('Workflow created successfully.');

    } catch (err) {

      setActionError(`Could not create workflow: ${err.message}`);

    } finally {

      setSaving(false);

    }

  }

  async function createRequirement(event) {

    event.preventDefault();

    if (!selectedWorkflowId) return;

    setSaving(true);

    setActionError('');

    setSuccess('');

    try {

      await apiRequest(

        `${API_URL}/${selectedWorkflowId}/requirements`,

        {

          method: 'POST',

          body: JSON.stringify({

            title: requirementForm.title.trim(),

            description: requirementForm.description.trim() || null,

            mandatory: requirementForm.mandatory,

            sortOrder: Number(requirementForm.sortOrder)

          })

        }

      );

      setRequirementForm(emptyRequirement);

      setShowRequirementForm(false);

      await loadWorkflowDetails(selectedWorkflowId);

      setSuccess('Requirement created successfully.');

    } catch (err) {

      setActionError(`Could not create requirement: ${err.message}`);

    } finally {

      setSaving(false);

    }

  }

  async function createEvidence(event) {

    event.preventDefault();

    const requirementId = evidenceFormFor;

    if (!selectedWorkflowId || !requirementId) return;

    setSaving(true);

    setActionError('');

    setSuccess('');

    try {

      await apiRequest(

        `${API_URL}/${selectedWorkflowId}/requirements/${requirementId}/evidence`,

        {

          method: 'POST',

          body: JSON.stringify({

            type: evidenceForm.type,

            title: evidenceForm.title.trim(),

            evidenceUrl: evidenceForm.evidenceUrl.trim(),

            description: evidenceForm.description.trim() || null,

            sourceVersion: evidenceForm.sourceVersion.trim() || null

          })

        }

      );

      setEvidenceForm(emptyEvidence);

      setEvidenceFormFor(null);

      await loadWorkflowDetails(selectedWorkflowId);

      await loadReadiness(selectedWorkflowId);

      setSuccess('Evidence submitted successfully. It is awaiting review.');

    } catch (err) {

      setActionError(`Could not submit evidence: ${err.message}`);

    } finally {

      setSaving(false);

    }

  }

  async function reviewEvidence(requirementId, evidenceId, status) {

    setActionError('');

    setSuccess('');

    try {

      await apiRequest(

        `${API_URL}/${selectedWorkflowId}/requirements/${requirementId}/evidence/${evidenceId}/review`,

        {

          method: 'PATCH',

          body: JSON.stringify({ status })

        }

      );

      await loadWorkflowDetails(selectedWorkflowId);

      await loadReadiness(selectedWorkflowId);

      setSuccess(`Evidence marked ${status.toLowerCase()}.`);

    } catch (err) {

      setActionError(`Could not review evidence: ${err.message}`);

    }

  }

  function getReadinessStatus(workflow) {
    const result = readinessByWorkflow[workflow.id];
    if (!result) return 'CHECKING';
    if (result.ready === true) return 'READY';
    if ((result.pendingRequirements?.length ?? 0) > 0) return 'REVIEW';
    return 'BLOCKED';
  }

  const filteredWorkflows = workflows.filter(item => {
    if (filter === 'All') return true;
    if (filter === 'DRAFT') return item.status === 'DRAFT';
    return getReadinessStatus(item) === filter;
  });

  const readyCount = workflows.filter(

    workflow => readinessByWorkflow[workflow.id]?.ready === true

  ).length;

  const reviewCount = workflows.filter(

    workflow => (readinessByWorkflow[workflow.id]?.pendingRequirements?.length ?? 0) > 0

  ).length;

  const blockedCount = workflows.filter(workflow => {

    const result = readinessByWorkflow[workflow.id];

    return result != null && result.ready !== true &&

      (result.pendingRequirements?.length ?? 0) === 0;

  }).length;

  const allEvidence = Object.values(evidenceByRequirement).flat();

  const pendingEvidenceCount = allEvidence.filter(

    item => item.status === 'PENDING'

  ).length;

  function renderWorkflowTable() {

    return (

      <section className="table-panel">

        <div className="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-3">

          <div>

            <h5>Workflow register</h5>

            <p className="text-secondary small mb-0">

              Live data from your Spring Boot backend.

            </p>

          </div>

          <div className="d-flex gap-2 flex-wrap">

            {['All', 'READY', 'REVIEW', 'BLOCKED', 'DRAFT'].map(status => (

              <button

                key={status}

                className={`btn btn-sm ${

                  filter === status ? 'btn-dark' : 'btn-outline-secondary'

                }`}

                onClick={() => setFilter(status)}

              >

                {status}

              </button>

            ))}

          </div>

        </div>

        {loading && <p>Loading workflows...</p>}

        {error && <p className="text-danger">{error}</p>}

        {!loading && !error && (

          <div className="table-responsive">

            <table className="table align-middle">

              <thead>

                <tr>

                  <th>WORKFLOW</th>

                  <th>VERSION</th>

                  <th>LIFECYCLE</th>

                  <th>RELEASE READINESS</th>

                  <th>ACTION</th>

                </tr>

              </thead>

              <tbody>

                {filteredWorkflows.map(item => (

                  <tr key={item.id}>

                    <td>

                      <strong>{item.name}</strong>

                      <small className="d-block text-secondary">

                        ID: {item.id}

                      </small>

                    </td>

                    <td>{item.currentVersion || '—'}</td>

                    <td>

                      <span

                        className={`status-pill ${String(item.status).toLowerCase()}`}

                      >

                        {item.status}

                      </span>

                    </td>

                    <td>

                      <span className={`status-pill ${getReadinessStatus(item).toLowerCase()}`}>

                        {getReadinessStatus(item) === 'READY'
                          ? 'READY FOR RELEASE'
                          : getReadinessStatus(item) === 'CHECKING'
                            ? 'CHECKING...'
                            : getReadinessStatus(item) === 'REVIEW'
                              ? 'AWAITING REVIEW'
                              : 'BLOCKED'}

                      </span>

                    </td>

                    <td>

                      <button

                        className="btn btn-sm btn-outline-dark"

                        onClick={() => {

                          setSelectedWorkflowId(String(item.id));

                          navigate('Workflows');

                        }}

                      >

                        View details

                      </button>

                    </td>

                  </tr>

                ))}

                {filteredWorkflows.length === 0 && (

                  <tr>

                    <td colSpan="5" className="text-center text-secondary py-4">

                      No workflows found for this status.

                    </td>

                  </tr>

                )}

              </tbody>

            </table>

          </div>

        )}

        {!loading && !error && (

          <small className="text-secondary">

            Showing {filteredWorkflows.length} of {workflows.length} workflows

          </small>

        )}

      </section>

    );

  }

  return (

    <div className="app-shell">

      <aside className="sidebar">

        <h4 className="brand">

          <i className="bi bi-shield-check me-2"></i>

          StepSentry<span>.</span>

        </h4>

        <small className="nav-label">WORKSPACE</small>

        {[

          ['Overview', 'bi-grid-1x2'],

          ['Workflows', 'bi-diagram-3'],

          ['Evidence', 'bi-file-earmark-check'],

          ['Readiness', 'bi-shield-lock']

        ].map(([label, icon]) => (

          <button

            key={label}

            type="button"

            className={`nav-link-custom ${page === label ? 'active' : ''}`}

            onClick={() => navigate(label)}

          >

            <i className={`bi ${icon}`}></i>

            {label}

          </button>

        ))}

        <div className="sidebar-bottom">

          <i className="bi bi-lock-fill"></i> Evidence-first releases

        </div>

      </aside>

      <main className="main-area">

        <header className="topbar">

          <span>WORKSPACE / {page.toUpperCase()}</span>

          <span className="live-dot">● SYSTEM ONLINE</span>

        </header>

        {page === 'Overview' && (

          <>

            <section className="page-heading">

              <div>

                <p className="eyebrow">RELEASE CONTROL CENTER</p>

                <h1>Ship with <span>confidence.</span></h1>

                <p className="text-secondary">

                  Know what's proven, what's missing, and what's holding your release back.

                </p>

              </div>

              <button

                className="btn btn-lime"

                onClick={loadWorkflows}

                disabled={loading}

              >

                <i className="bi bi-arrow-repeat me-2"></i>

                Refresh workflows

              </button>

            </section>

            <section className="row g-3 mb-4">

              {[

                ['bi-diagram-3', 'Total workflows', workflows.length],

                ['bi-shield-check', 'Release ready', readyCount],

                ['bi-hourglass-split', 'Awaiting review', reviewCount],

                ['bi-exclamation-triangle', 'Blocked workflows', blockedCount]

              ].map(([icon, label, value]) => (

                <div className="col-6 col-xl-3" key={label}>

                  <div className="metric-card">

                    <i className={`bi ${icon} metric-icon`}></i>

                    <h2>{loading ? '—' : value}</h2>

                    <p>{label}</p>

                  </div>

                </div>

              ))}

            </section>

            <section className="readiness-banner mb-4">

              <i className="bi bi-shield-check readiness-icon"></i>

              <div className="flex-grow-1">

                <p className="eyebrow">RELEASE HEALTH</p>

                <h4>Evidence before deployment.</h4>

                <p>Every mandatory requirement needs current, approved evidence.</p>

              </div>

              <div className="health-score">

                <strong>

                  {loading

                    ? '—'

                    : `${workflows.length

                        ? Math.round((readyCount / workflows.length) * 100)

                        : 0}%`}

                </strong>

                <small>WORKFLOW STATUS</small>

              </div>

            </section>

            {renderWorkflowTable()}

          </>

        )}

        {page === 'Workflows' && (

          <>

            <section className="page-heading">

              <div>

                <p className="eyebrow">WORKFLOW MANAGEMENT</p>

                <h1>Workflow <span>details.</span></h1>

                <p className="text-secondary">

                  Manage requirements and evidence for each release workflow.

                </p>

              </div>

              <button

                className="btn btn-lime"

                onClick={() => {

                  setShowWorkflowForm(value => !value);

                  setActionError('');

                  setSuccess('');

                }}

              >

                <i className="bi bi-plus-lg me-2"></i>

                Create workflow

              </button>

            </section>

            {showWorkflowForm && (

              <section className="table-panel mb-4">

                <h5 className="mb-3">Create workflow</h5>

                <form onSubmit={createWorkflow}>

                  <div className="mb-3">

                    <label className="form-label">Workflow name *</label>

                    <input

                      className="form-control"

                      required

                      maxLength={150}

                      value={workflowForm.name}

                      onChange={e =>

                        setWorkflowForm({ ...workflowForm, name: e.target.value })

                      }

                      placeholder="e.g. Release 3.0"

                    />

                  </div>

                  <div className="mb-3">

                    <label className="form-label">Description</label>

                    <textarea

                      className="form-control"

                      rows="2"

                      value={workflowForm.description}

                      onChange={e =>

                        setWorkflowForm({

                          ...workflowForm,

                          description: e.target.value

                        })

                      }

                    />

                  </div>

                  <div className="mb-3">

                    <label className="form-label">Current version</label>

                    <input

                      className="form-control"

                      value={workflowForm.currentVersion}

                      onChange={e =>

                        setWorkflowForm({

                          ...workflowForm,

                          currentVersion: e.target.value

                        })

                      }

                      placeholder="e.g. v1.0"

                    />

                  </div>

                  <button className="btn btn-lime" disabled={saving}>

                    {saving ? 'Creating...' : 'Save workflow'}

                  </button>

                </form>

              </section>

            )}

            {renderWorkflowTable()}

            {selectedWorkflow && (

              <section className="table-panel mt-4">

                <div className="d-flex justify-content-between align-items-start flex-wrap gap-3 mb-4">

                  <div>

                    <p className="eyebrow">SELECTED WORKFLOW</p>

                    <h4>{selectedWorkflow.name}</h4>

                    <p className="text-secondary mb-1">

                      {selectedWorkflow.description || 'No description provided.'}

                    </p>

                    <small className="text-secondary">

                      ID: {selectedWorkflow.id} · Version:{' '}

                      {selectedWorkflow.currentVersion || '—'} · Status:{' '}

                      {selectedWorkflow.status}

                    </small>

                  </div>

                  <div style={{ minWidth: 220 }}>

                    <label className="form-label">Choose workflow</label>

                    <select

                      className="form-select"

                      value={selectedWorkflowId}

                      onChange={e => setSelectedWorkflowId(e.target.value)}

                    >

                      {workflows.map(workflow => (

                        <option key={workflow.id} value={workflow.id}>

                          {workflow.name} (#{workflow.id})

                        </option>

                      ))}

                    </select>

                  </div>

                </div>

                <div className="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-3">

                  <div>

                    <h5 className="mb-1">Requirements</h5>

                    <p className="text-secondary small mb-0">

                      {requirements.length} requirement(s) ·{' '}

                      {requirements.filter(r => r.mandatory).length} mandatory

                    </p>

                  </div>

                  <button

                    className="btn btn-lime"

                    onClick={() => {

                      setRequirementForm({

                        ...emptyRequirement,

                        sortOrder: requirements.length

                      });

                      setShowRequirementForm(value => !value);

                      setActionError('');

                      setSuccess('');

                    }}

                  >

                    <i className="bi bi-plus-lg me-2"></i>

                    Add requirement

                  </button>

                </div>

                {showRequirementForm && (

                  <form

                    className="border rounded p-3 mb-4"

                    onSubmit={createRequirement}

                  >

                    <h6 className="mb-3">New requirement</h6>

                    <div className="mb-3">

                      <label className="form-label">Title *</label>

                      <input

                        className="form-control"

                        required

                        value={requirementForm.title}

                        onChange={e =>

                          setRequirementForm({

                            ...requirementForm,

                            title: e.target.value

                          })

                        }

                        placeholder="e.g. Automated tests pass"

                      />

                    </div>

                    <div className="mb-3">

                      <label className="form-label">Description</label>

                      <textarea

                        className="form-control"

                        rows="2"

                        value={requirementForm.description}

                        onChange={e =>

                          setRequirementForm({

                            ...requirementForm,

                            description: e.target.value

                          })

                        }

                      />

                    </div>

                    <div className="row g-3 mb-3">

                      <div className="col-md-6">

                        <label className="form-label">Sort order</label>

                        <input

                          className="form-control"

                          type="number"

                          min="0"

                          value={requirementForm.sortOrder}

                          onChange={e =>

                            setRequirementForm({

                              ...requirementForm,

                              sortOrder: e.target.value

                            })

                          }

                        />

                      </div>

                      <div className="col-md-6 d-flex align-items-end">

                        <div className="form-check mb-2">

                          <input

                            id="mandatoryRequirement"

                            className="form-check-input"

                            type="checkbox"

                            checked={requirementForm.mandatory}

                            onChange={e =>

                              setRequirementForm({

                                ...requirementForm,

                                mandatory: e.target.checked

                              })

                            }

                          />

                          <label

                            className="form-check-label"

                            htmlFor="mandatoryRequirement"

                          >

                            Mandatory requirement

                          </label>

                        </div>

                      </div>

                    </div>

                    <button className="btn btn-lime me-2" disabled={saving}>

                      {saving ? 'Saving...' : 'Save requirement'}

                    </button>

                    <button

                      type="button"

                      className="btn btn-outline-secondary"

                      onClick={() => setShowRequirementForm(false)}

                    >

                      Cancel

                    </button>

                  </form>

                )}

                {detailsLoading && <p>Loading requirements and evidence...</p>}

                {!detailsLoading && requirements.length === 0 && (

                  <div className="text-center text-secondary py-4">

                    <i className="bi bi-list-check fs-2"></i>

                    <p className="mt-2 mb-0">No requirements yet.</p>

                    <small>Add a requirement to start tracking release evidence.</small>

                  </div>

                )}

                {!detailsLoading &&

                  [...requirements]

                    .sort((a, b) => a.sortOrder - b.sortOrder)

                    .map(requirement => {

                      const evidence =

                        evidenceByRequirement[requirement.id] || [];

                      return (

                        <div

                          key={requirement.id}

                          className="border rounded p-3 mb-3"

                        >

                          <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">

                            <div>

                              <h6 className="mb-1">

                                {requirement.sortOrder + 1}. {requirement.title}

                              </h6>

                              <p className="text-secondary small mb-2">

                                {requirement.description ||

                                  'No description provided.'}

                              </p>

                            </div>

                            <span

                              className={`badge ${

                                requirement.mandatory

                                  ? 'text-bg-warning'

                                  : 'text-bg-secondary'

                              }`}

                            >

                              {requirement.mandatory ? 'Mandatory' : 'Optional'}

                            </span>

                          </div>

                          <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 mt-2 mb-3">

                            <small className="text-secondary">

                              {evidence.length} evidence item(s)

                            </small>

                            <button

                              className="btn btn-sm btn-outline-dark"

                              onClick={() => {

                                setEvidenceForm(emptyEvidence);

                                setEvidenceFormFor(current =>

                                  current === requirement.id

                                    ? null

                                    : requirement.id

                                );

                                setActionError('');

                                setSuccess('');

                              }}

                            >

                              <i className="bi bi-plus-lg me-1"></i>

                              Add evidence

                            </button>

                          </div>

                          {evidenceFormFor === requirement.id && (

                            <form

                              className="border rounded p-3 mb-3"

                              onSubmit={createEvidence}

                            >

                              <h6 className="mb-3">Submit evidence</h6>

                              <div className="mb-3">

                                <label className="form-label">Evidence type *</label>

                                <select

                                  className="form-select"

                                  value={evidenceForm.type}

                                  onChange={e =>

                                    setEvidenceForm({

                                      ...evidenceForm,

                                      type: e.target.value

                                    })

                                  }

                                >

                                  <option value="PULL_REQUEST">Pull request</option>

                                  <option value="TEST_REPORT">Test report</option>

                                  <option value="DOCUMENT">Document</option>

                                  <option value="OTHER">Other</option>

                                </select>

                              </div>

                              <div className="mb-3">

                                <label className="form-label">Title *</label>

                                <input

                                  className="form-control"

                                  required

                                  value={evidenceForm.title}

                                  onChange={e =>

                                    setEvidenceForm({

                                      ...evidenceForm,

                                      title: e.target.value

                                    })

                                  }

                                  placeholder="e.g. Unit test results"

                                />

                              </div>

                              <div className="mb-3">

                                <label className="form-label">Evidence URL *</label>

                                <input

                                  className="form-control"

                                  type="url"

                                  required

                                  pattern="https?://.+"

                                  title="Enter a URL starting with http:// or https://"

                                  value={evidenceForm.evidenceUrl}

                                  onChange={e =>

                                    setEvidenceForm({

                                      ...evidenceForm,

                                      evidenceUrl: e.target.value

                                    })

                                  }

                                  placeholder="https://..."

                                />

                              </div>

                              <div className="mb-3">

                                <label className="form-label">Description</label>

                                <textarea

                                  className="form-control"

                                  rows="2"

                                  value={evidenceForm.description}

                                  onChange={e =>

                                    setEvidenceForm({

                                      ...evidenceForm,

                                      description: e.target.value

                                    })

                                  }

                                />

                              </div>

                              <div className="mb-3">

                                <label className="form-label">Source version</label>

                                <input

                                  className="form-control"

                                  value={evidenceForm.sourceVersion}

                                  onChange={e =>

                                    setEvidenceForm({

                                      ...evidenceForm,

                                      sourceVersion: e.target.value

                                    })

                                  }

                                  placeholder={selectedWorkflow.currentVersion || 'e.g. v2.0'}

                                />

                              </div>

                              <button className="btn btn-lime me-2" disabled={saving}>

                                {saving ? 'Submitting...' : 'Submit evidence'}

                              </button>

                              <button

                                type="button"

                                className="btn btn-outline-secondary"

                                onClick={() => setEvidenceFormFor(null)}

                              >

                                Cancel

                              </button>

                            </form>

                          )}

                          {evidence.length > 0 && (

                            <div className="table-responsive">

                              <table className="table table-sm align-middle mb-0">

                                <thead>

                                  <tr>

                                    <th>EVIDENCE</th>

                                    <th>TYPE</th>

                                    <th>STATUS</th>

                                    <th>REVIEW</th>

                                  </tr>

                                </thead>

                                <tbody>

                                  {evidence.map(item => (

                                    <tr key={item.id}>

                                      <td>

                                        <strong>{item.title}</strong>

                                        <a

                                          className="d-block small"

                                          href={item.evidenceUrl}

                                          target="_blank"

                                          rel="noreferrer"

                                        >

                                          Open evidence

                                        </a>

                                        {item.sourceVersion && (

                                          <small className="text-secondary">

                                            Version: {item.sourceVersion}

                                          </small>

                                        )}

                                      </td>

                                      <td>{item.type.replaceAll('_', ' ')}</td>

                                      <td>

                                        <span

                                          className={`status-pill ${String(item.status).toLowerCase()}`}

                                        >

                                          {item.status}

                                        </span>

                                      </td>

                                      <td>

                                        {item.status === 'PENDING' ? (

                                          <div className="d-flex gap-1 flex-wrap">

                                            <button

                                              className="btn btn-sm btn-success"

                                              onClick={() =>

                                                reviewEvidence(

                                                  requirement.id,

                                                  item.id,

                                                  'APPROVED'

                                                )

                                              }

                                            >

                                              Approve

                                            </button>

                                            <button

                                              className="btn btn-sm btn-outline-danger"

                                              onClick={() =>

                                                reviewEvidence(

                                                  requirement.id,

                                                  item.id,

                                                  'REJECTED'

                                                )

                                              }

                                            >

                                              Reject

                                            </button>

                                          </div>

                                        ) : (

                                          <small className="text-secondary">

                                            Reviewed

                                          </small>

                                        )}

                                      </td>

                                    </tr>

                                  ))}

                                </tbody>

                              </table>

                            </div>

                          )}

                        </div>

                      );

                    })}

              </section>

            )}

          </>

        )}

        {page === 'Evidence' && (
          <>
            <section className="page-heading">
              <div>
                <p className="eyebrow">EVIDENCE MANAGEMENT</p>
                <h1>Review <span>evidence.</span></h1>
                <p className="text-secondary">
                  View evidence, check its version, and review submissions for the selected workflow.
                </p>
              </div>
              <button
                className="btn btn-lime"
                onClick={() => navigate('Workflows')}
              >
                Manage requirements
              </button>
            </section>

            <section className="table-panel mb-4">
              <label className="form-label" htmlFor="evidenceWorkflowSelect">Choose workflow</label>
              <select
                id="evidenceWorkflowSelect"
                className="form-select"
                value={selectedWorkflowId}
                onChange={e => setSelectedWorkflowId(e.target.value)}
                disabled={workflows.length === 0}
              >
                {workflows.length === 0 && <option value="">No workflows available</option>}
                {workflows.map(workflow => (
                  <option key={workflow.id} value={workflow.id}>
                    {workflow.name} (#{workflow.id})
                  </option>
                ))}
              </select>
              {selectedWorkflow && (
                <p className="text-secondary small mt-2 mb-0">
                  Current version: <strong>{selectedWorkflow.currentVersion || 'Not specified'}</strong>
                  {' · '}Lifecycle: <strong>{selectedWorkflow.status}</strong>
                </p>
              )}
            </section>

            <section className="row g-3 mb-4">
              <div className="col-md-6">
                <div className="metric-card">
                  <i className="bi bi-file-earmark-check metric-icon"></i>
                  <h2>{allEvidence.length}</h2>
                  <p>Evidence items in selected workflow</p>
                </div>
              </div>
              <div className="col-md-6">
                <div className="metric-card">
                  <i className="bi bi-hourglass-split metric-icon"></i>
                  <h2>{pendingEvidenceCount}</h2>
                  <p>Awaiting review</p>
                </div>
              </div>
            </section>

            {actionError && <div className="alert alert-danger" role="alert">{actionError}</div>}
            {success && <div className="alert alert-success" role="status">{success}</div>}
            {detailsLoading && <p className="text-secondary">Loading evidence...</p>}

            {!detailsLoading && !selectedWorkflow && (
              <section className="table-panel text-center">
                <h5>No workflow available</h5>
                <p className="text-secondary">Create a workflow before adding evidence.</p>
                <button className="btn btn-lime" onClick={() => navigate('Workflows')}>
                  Open workflows
                </button>
              </section>
            )}

            {!detailsLoading && selectedWorkflow && requirements.length === 0 && (
              <section className="table-panel text-center">
                <h5>No requirements yet</h5>
                <p className="text-secondary">Add a requirement to this workflow before submitting evidence.</p>
                <button className="btn btn-lime" onClick={() => navigate('Workflows')}>
                  Add requirements
                </button>
              </section>
            )}

            {!detailsLoading && selectedWorkflow && requirements.length > 0 && (
              <section className="table-panel">
                <h5 className="mb-3">Evidence by requirement</h5>
                {[...requirements]
                  .sort((a, b) => a.sortOrder - b.sortOrder)
                  .map(requirement => {
                    const evidenceItems = evidenceByRequirement[requirement.id] || [];
                    return (
                      <div className="border rounded p-3 mb-3" key={requirement.id}>
                        <div className="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">
                          <div>
                            <h6 className="mb-1">{requirement.title}</h6>
                            <small className="text-secondary">
                              {requirement.mandatory ? 'Mandatory' : 'Optional'} requirement · {evidenceItems.length} evidence item(s)
                            </small>
                          </div>
                          <button
                            className="btn btn-sm btn-outline-dark"
                            onClick={() => navigate('Workflows')}
                          >
                            <i className="bi bi-pencil-square me-1"></i>Manage requirement
                          </button>
                        </div>

                        {evidenceItems.length === 0 ? (
                          <p className="text-secondary mb-0">No evidence submitted for this requirement yet.</p>
                        ) : (
                          <div className="table-responsive">
                            <table className="table table-sm align-middle mb-0">
                              <thead>
                                <tr>
                                  <th>Evidence</th>
                                  <th>Type</th>
                                  <th>Status</th>
                                  <th>Version check</th>
                                  <th>Review</th>
                                </tr>
                              </thead>
                              <tbody>
                                {evidenceItems.map(item => {
                                  const hasSourceVersion = Boolean(item.sourceVersion);
                                  const isCurrentVersion = hasSourceVersion &&
                                    Boolean(selectedWorkflow.currentVersion) &&
                                    item.sourceVersion === selectedWorkflow.currentVersion;
                                  const versionLabel = !hasSourceVersion
                                    ? 'Not specified'
                                    : !selectedWorkflow.currentVersion
                                      ? `Source: ${item.sourceVersion}`
                                      : isCurrentVersion
                                        ? 'Current'
                                        : `Outdated (${item.sourceVersion})`;
                                  return (
                                    <tr key={item.id}>
                                      <td>
                                        <strong>{item.title}</strong>
                                        <a className="d-block small" href={item.evidenceUrl} target="_blank" rel="noreferrer">
                                          Open evidence
                                        </a>
                                      </td>
                                      <td>{String(item.type || 'OTHER').replaceAll('_', ' ')}</td>
                                      <td>
                                        <span className={`status-pill ${String(item.status).toLowerCase()}`}>
                                          {item.status}
                                        </span>
                                      </td>
                                      <td>
                                        <span className={isCurrentVersion ? 'text-success' : hasSourceVersion && selectedWorkflow.currentVersion ? 'text-warning' : 'text-secondary'}>
                                          {versionLabel}
                                        </span>
                                      </td>
                                      <td>
                                        {item.status === 'PENDING' ? (
                                          <div className="d-flex gap-1 flex-wrap">
                                            <button className="btn btn-sm btn-success" disabled={saving} onClick={() => reviewEvidence(requirement.id, item.id, 'APPROVED')}>
                                              Approve
                                            </button>
                                            <button className="btn btn-sm btn-outline-danger" disabled={saving} onClick={() => reviewEvidence(requirement.id, item.id, 'REJECTED')}>
                                              Reject
                                            </button>
                                          </div>
                                        ) : (
                                          <small className="text-secondary">Reviewed</small>
                                        )}
                                      </td>
                                    </tr>
                                  );
                                })}
                              </tbody>
                            </table>
                          </div>
                        )}
                      </div>
                    );
                  })}
              </section>
            )}
          </>
        )}

        {page === 'Readiness' && (

        <>

          <section className="page-heading">

            <div>

              <p className="eyebrow">RELEASE ASSURANCE</p>

              <h1>Check <span>readiness.</span></h1>

              <p className="text-secondary">Official release-readiness results calculated by the StepSentry backend.</p>

            </div>

            <button className="btn btn-lime" onClick={() => loadReadiness()} disabled={!selectedWorkflowId || readinessLoading}>

              <i className="bi bi-arrow-repeat me-2"></i>

              {readinessLoading ? 'Refreshing...' : 'Refresh results'}

            </button>

          </section>

          <section className="table-panel">

            {!selectedWorkflow ? (

              <p>No workflow is available yet. Create a workflow first.</p>

            ) : (

              <>

                <label className="form-label">Choose workflow</label>

                <select className="form-select mb-3" value={selectedWorkflowId} onChange={e => setSelectedWorkflowId(e.target.value)}>

                  {workflows.map(workflow => <option key={workflow.id} value={workflow.id}>{workflow.name} (#{workflow.id})</option>)}

                </select>

                <div className="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-3">

                  <div>

                    <h4 className="mb-1">{selectedWorkflow.name}</h4>

                    <p className="text-secondary mb-0">Version: {readiness?.workflowVersion || selectedWorkflow.currentVersion || 'Not specified'}</p>

                  </div>

                  {readiness && <span className={`badge ${readiness.ready ? 'text-bg-success' : 'text-bg-danger'} fs-6 p-2`}>{readiness.ready ? 'READY FOR RELEASE' : 'NOT READY'}</span>}

                </div>

                {readinessLoading && <p>Loading official readiness results...</p>}

                {readinessError && <div className="alert alert-danger" role="alert">{readinessError}</div>}

                {!readinessLoading && readiness && (

                  <>

                    <div className="row g-3 mb-4">

                      {[

                        ['Total requirements', readiness.summary?.totalRequirements],

                        ['Mandatory', readiness.summary?.mandatoryRequirements],

                        ['Satisfied', readiness.summary?.satisfiedMandatoryRequirements],

                        ['Missing', readiness.summary?.missingMandatoryRequirements],

                        ['Stale', readiness.summary?.staleMandatoryRequirements],

                        ['Pending review', readiness.summary?.pendingMandatoryRequirements],

                        ['Rejected', readiness.summary?.rejectedMandatoryRequirements]

                      ].map(([label, value]) => (

                        <div className="col-6 col-md-4 col-xl-3" key={label}>

                          <div className="metric-card"><h2>{value ?? 0}</h2><p>{label}</p></div>

                        </div>

                      ))}

                    </div>

                    <p className="text-secondary small">Backend workflow status: <strong>{readiness.status || 'Unknown'}</strong></p>

                    <div className={`alert ${readiness.ready ? 'alert-success' : 'alert-warning'} py-2 small`} role="status">

                      {readiness.ready

                        ? 'All mandatory requirements are satisfied with current, approved evidence. Readiness does not publish the workflow; its lifecycle status remains separate until it is formally released.'

                        : 'This workflow is not ready for release yet. Review the missing, stale, pending, or rejected requirement checks below. Readiness and lifecycle status are tracked separately.'}

                    </div>

                    {[

                      { key: 'missingRequirements', title: 'Missing requirements', countKey: 'missingEvidenceCount' },

                      { key: 'staleRequirements', title: 'Stale requirements', countKey: 'staleEvidenceCount' },

                      { key: 'pendingRequirements', title: 'Requirements awaiting review', countKey: 'pendingEvidenceCount' },

                      { key: 'rejectedRequirements', title: 'Requirements with rejected evidence', countKey: 'rejectedEvidenceCount' }

                    ].map(group => {

                      const items = readiness[group.key] || [];

                      return (

                        <div className="border rounded p-3 mb-3" key={group.key}>

                          <div className="d-flex justify-content-between align-items-center mb-2"><h5 className="mb-0">{group.title}</h5><span className="badge text-bg-secondary">{items.length}</span></div>

                          {items.length === 0 ? <p className="text-secondary small mb-0">None</p> : (

                            <ul className="mb-0">

                              {items.map(item => (

                                <li key={item.requirementId} className="mb-1">

                                  <strong>{item.title || `Requirement #${item.requirementId}`}</strong>

                                  {item[group.countKey] != null && <span className="text-secondary"> — {item[group.countKey]} evidence item(s)</span>}

                                </li>

                              ))}

                            </ul>

                          )}

                        </div>

                      );

                    })}

                    <div className="table-responsive mt-4">

                      <h5>All requirement checks</h5>

                      <table className="table align-middle">

                        <thead><tr><th>REQUIREMENT</th><th>MANDATORY</th><th>RESULT</th><th>FRESHNESS</th><th>EVIDENCE</th></tr></thead>

                        <tbody>

                          {(readiness.requirements || []).map(item => (

                            <tr key={item.requirementId}>

                              <td>{item.title}</td><td>{item.mandatory ? 'Yes' : 'No'}</td><td>{item.satisfied ? 'Satisfied' : 'Not satisfied'}</td>

                              <td>{String(item.freshness || 'UNKNOWN').replaceAll('_', ' ')}</td>

                              <td>{item.approvedEvidenceCount ?? 0} approved / {item.evidenceCount ?? 0} total

                                <small className="d-block text-secondary">{item.pendingEvidenceCount ?? 0} pending · {item.rejectedEvidenceCount ?? 0} rejected · {item.staleEvidenceCount ?? 0} stale</small>

                              </td>

                            </tr>

                          ))}

                          {(readiness.requirements || []).length === 0 && <tr><td colSpan="5" className="text-secondary">No requirements have been added to this workflow.</td></tr>}

                        </tbody>

                      </table>

                    </div>

                  </>

                )}

              </>

            )}

          </section>

        </>

      )}

      {actionError && (

          <div className="alert alert-danger mt-3" role="alert">

            {actionError}

          </div>

        )}

        {success && (

          <div className="alert alert-success mt-3" role="status">

            {success}

          </div>

        )}

        <footer>

          Built for evidence-based release confidence.

          <span>STEP SMART. SHIP SAFE.</span>

        </footer>

      </main>

    </div>

  );

}

export default App;
