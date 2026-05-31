#!/bin/bash
# ══════════════════════════════════════════════════════════
#  AcademyAI – Test Runner Script
#  Usage:
#    ./run.sh               → full regression suite
#    ./run.sh smoke         → smoke tests only
#    ./run.sh auth          → auth tests only
#    ./run.sh ai            → AI tests only
#    ./run.sh chat          → chat tests only
#    ./run.sh social        → social tests only
#    ./run.sh schedule      → schedule tests only
#    ./run.sh performance   → performance tests only
#    ./run.sh report        → open Allure report
# ══════════════════════════════════════════════════════════

set -e
CYAN='\033[0;36m'; GREEN='\033[0;32m'; RED='\033[0;31m'; NC='\033[0m'
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

print_banner() {
  echo -e "${CYAN}"
  echo "╔══════════════════════════════════════════════════╗"
  echo "║   AcademyAI – API Automation Framework           ║"
  echo "║   Stack: Rest Assured | TestNG | Allure | Maven  ║"
  echo "║   Time: $TIMESTAMP                    ║"
  echo "╚══════════════════════════════════════════════════╝"
  echo -e "${NC}"
}

run_tests() {
  local PROFILE="$1"
  local LABEL="$2"
  echo -e "${CYAN}[RUN] $LABEL${NC}"
  mvn clean test -P "$PROFILE" \
    -Dtest.timestamp="$TIMESTAMP" \
    2>&1 | tee "test-run-$TIMESTAMP.log"
}

run_by_group() {
  local GROUP="$1"
  echo -e "${CYAN}[RUN] Group: $GROUP${NC}"
  mvn clean test \
    -Dgroups="$GROUP" \
    -Dtest.timestamp="$TIMESTAMP" \
    2>&1 | tee "test-run-$GROUP-$TIMESTAMP.log"
}

generate_report() {
  echo -e "${CYAN}[REPORT] Generating Allure report...${NC}"
  mvn allure:report
  echo -e "${GREEN}[DONE] Report at: target/site/allure-maven-plugin/index.html${NC}"
}

open_report() {
  mvn allure:serve
}

print_banner

case "${1:-all}" in
  smoke)       run_tests "smoke" "Smoke Tests" ;;
  auth)        run_by_group "auth" ;;
  ai)          run_by_group "ai" ;;
  chat)        run_by_group "chat" ;;
  social)      run_by_group "social" ;;
  schedule)    run_by_group "schedule" ;;
  performance) run_by_group "performance" ;;
  report)      generate_report ;;
  serve)       open_report ;;
  all)         run_tests "regression" "Full Regression Suite" ;;
  *)
    echo -e "${RED}Unknown command: $1${NC}"
    echo "Usage: ./run.sh [smoke|auth|ai|chat|social|schedule|performance|report|serve|all]"
    exit 1 ;;
esac

echo -e "${GREEN}[DONE] Completed at $(date)${NC}"
generate_report
