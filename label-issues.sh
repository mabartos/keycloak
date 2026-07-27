#!/bin/bash

REPO="keycloak/keycloak"
LABEL="release/26.8.0"
BACKPORT_LABEL="backport/main"
RELEASE_TIME="2026-07-09T06:58:13Z"
DRY_RUN=false

if [ "$1" == "--dry-run" ]; then
  DRY_RUN=true
  echo "=== DRY RUN ==="
fi

# Ensure the label exists
gh api "/repos/$REPO/labels/$LABEL" --silent 2>/dev/null || gh label create -R "$REPO" "$LABEL" -c "0E8A16"

# Get PR numbers from failed labeler runs after 26.7.0 release
echo "Fetching failed labeler runs..."
prs=$(gh run list --repo "$REPO" --workflow label.yml --status failure --limit 100 --json headBranch,createdAt --jq ".[] | select(.createdAt > \"$RELEASE_TIME\") | .headBranch" | while read -r branch; do
  gh pr list --repo "$REPO" --state merged --base main --head "$branch" --json number --jq '.[0].number // empty' 2>/dev/null
done | sort -n -u)

pr_count=$(echo "$prs" | grep -c '[0-9]')
echo "Found $pr_count merged PRs from failed runs"

labeled=0
skipped=0

for pr in $prs; do
  issues=$(.github/scripts/pr-find-issues.sh "$pr" "$REPO")

  for issue in $issues; do
    issue_json=$(gh api "/repos/$REPO/issues/$issue" --jq '{closed_at, labels: [.labels[].name]}' 2>/dev/null)
    closed_at=$(echo "$issue_json" | jq -r '.closed_at // empty')
    has_label=$(echo "$issue_json" | jq -r ".labels | index(\"$LABEL\") // empty")

    # Skip issues that already have the label
    if [ -n "$has_label" ]; then
      echo "Skipping #$issue (from PR #$pr) - already has $LABEL"
      skipped=$((skipped + 1))
      continue
    fi

    # Skip issues not closed after the 26.7.0 release
    if [ -z "$closed_at" ] || [[ "$closed_at" < "$RELEASE_TIME" ]]; then
      echo "Skipping #$issue (from PR #$pr) - not closed after 26.7.0 release"
      skipped=$((skipped + 1))
      continue
    fi

    if [ "$DRY_RUN" = true ]; then
      echo "Would label #$issue with $LABEL (from PR #$pr)"
    else
      echo "Labeling #$issue with $LABEL (from PR #$pr)"
      gh issue edit "$issue" -R "$REPO" --add-label "$LABEL" --remove-label "$BACKPORT_LABEL" 2>/dev/null
    fi
    labeled=$((labeled + 1))
  done
done

echo ""
if [ "$DRY_RUN" = true ]; then
  echo "Done. Would have labeled $labeled issues."
else
  echo "Done. Labeled $labeled issues."
fi
