# Task: Ulam Pattern Search

## Tools

- name: ulam_verifier
  description: "Validates Ulam distance constraints using Python."
  run: python3 droid/tools/ulam_verifier.py
  input: json
  output: json

## Agents

- reasoning
- execution

## Parameters

array: []
minDist: 2
permLength: 8

## Steps

### Step 1 (Reasoning)

Generate 5 candidate permutations of length {{ permLength }} that could potentially extend the current array (which must have minimum Ulam distance {{ minDist }}).
Return the candidates strictly as a JSON list of lists, like:
[[1,2,3,4,5,6,7,8], [2,1,3,4,5,6,7,8], ...]

### Step 2 (Execution)

Call the ulam_verifier tool with:
{
"mode": "extend",
"candidates": {{ last.reasoning_output }},
"array": {{ array }},
"minDist": {{ minDist }}
}

Store the returned "accepted" permutations in a variable.

### Step 3 (Reasoning)

Analyze the accepted permutations.
If none were accepted:

- Return a final explanation of why the search stopped.
- End the task.

If some were accepted:

- Add them to the working array (state update).
- Based on these, propose refined structural rules.
- Generate 5 new candidate permutations that match the refined rules.

### Loop

Repeat Steps 1–3 until no new permutations are accepted.
