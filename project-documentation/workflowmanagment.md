Jira Workflow Management
It is essential to maintain the integrity of task tracking. Once a ticket progresses through the workflow states (e.g., from Pending Dev QA to Completed), it must never be moved back to a previous state. Moving tickets backward causes confusion and results in loss of visibility into the actual release status.

If a bug is found during the Pending Test QA phase (or later), the standard procedure—which must be followed strictly—is the following:

Comment on the original ticket describing the test that revealed the bug.

Open a related ticket of type sub-bug (you may clone the original one).

Set the new ticket to TO ESTIMATE (never place it in the Backlog).

Carefully verify the Components and the correct assignment to the relevant Team Leader.

Resolution: The fix must originate from the develop branch; this ensures the correction naturally propagates to the environment where the original ticket resides.

Following this flow is the only way to avoid confusion within the team and to maintain a clean and reliable history.

flowchart TD

    A[Backlog] -->|task defined and described| B[To Estimate]

    B -->|estimated & added to sprint| C[TO DO]

    C -->|taken in charge| D[IN PROGRESS]

    D -->|pull request opened| E[PENDING DEV QA]

    E -->|approved| F[PENDING TEST QA]

    F -->|tests passed| G[READY FOR PROD]

    G -->|deploy in prod| H[DONE]

    %% Error handling (bug flow)
    F -->|bug found| X[SUB-BUG → TO ESTIMATE]
    G -->|error in prod| X

    %% Optional blocked states
    C --> Z[BLOCKED]
    D --> Z
    E --> Z

    %% Rule: no backward transitions

