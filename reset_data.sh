#!/bin/sh
cd "$(dirname "$0")"
rm -f data/accounts.txt data/complaints.txt data/groups.txt data/categories.txt
echo "Old data removed. Run ./run_demo.sh - fresh data is created from data/import/*.csv"
