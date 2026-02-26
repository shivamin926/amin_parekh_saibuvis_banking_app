# Test Failure Analysis & Resolution Log

**Last Updated:** 19 failing tests remaining (65/84 passing - 77.4%)

| Test ID | Status | Reason for Failure | Solution Applied | Notes |
|---------|--------|-------------------|------------------|-------|
| TC12 | ✅ FIXED | Wrong account owner (Alice instead of Jake) | Updated seed accounts to match currentaccounts.txt: 00002→Jake | Now correctly rejects with "name/account mismatch" |
| TC16 | ✅ FIXED | Invalid amount "abc" causes unwanted reprompt | Implemented parseAmountSilentReprompt for format errors with silent reprompting (no READ_CMD logging) | Now correctly reprompts on invalid format without logging intermediate reads |
| TC20 | ✅ FIXED | Wrong error message for transfer name mismatch | Changed "ERROR: source account name mismatch" to "ERROR: name/account mismatch" | Now matches expected error message format |
| TC25 | ❓ PENDING | Not analyzed | Not yet | - |
| TC29 | ❓ PENDING | Not analyzed | Not yet | - |
| TC34 | ❓ PENDING | Not analyzed | Not yet | - |
| TC38 | ❓ PENDING | Not analyzed | Not yet | - |
| TC39 | ❓ PENDING | Not analyzed | Not yet | - |
| TC40 | ❓ PENDING | Not analyzed | Not yet | - |
| TC41 | ❓ PENDING | Not analyzed | Not yet | - |
| TC42 | ❓ PENDING | Not analyzed | Not yet | - |
| TC43 | ❓ PENDING | Not analyzed | Not yet | - |
| TC48 | ❓ PENDING | Not analyzed | Not yet | - |
| TC54 | ❓ PENDING | Not analyzed | Not yet | - |
| TC56 | ❓ PENDING | Not analyzed | Not yet | - |
| TC59 | ❓ PENDING | Not analyzed | Not yet | - |
| TC66 | ❓ PENDING | Not analyzed | Not yet | - |
| TC67 | ❓ PENDING | Not analyzed | Not yet | - |
| TC68 | ❓ PENDING | Not analyzed | Not yet | - |
| TC72 | ❓ PENDING | Not analyzed | Not yet | - |
| TC74 | ❓ PENDING | Not analyzed | Not yet | - |
| TC76 | ❓ PENDING | Not analyzed | Not yet | - |

### Previously Fixed (now passing)
- **TC04**: Unknown command without login → Added check for unknown commands requiring login
- **TC06**: Error message "admin access required" → Changed to "insufficient privilege"
- **TC44**: Non-positive amounts → Added amount validation with reprompting
- **TC50, TC51, TC52**: Various fixes from earlier iterations
- **And 13 others...**

---

## Summary
- **Total Tests:** 84
- **Passing:** 65 ✅
- **Failing:** 19
- **Pass Rate:** 77.4%



