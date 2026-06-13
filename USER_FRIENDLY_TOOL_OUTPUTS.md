# Conversational Tool Output & Profile Filter Design

To ensure the AI Agent communicates conversationally and avoids outputting raw JSON array payloads or system-level stack traces directly in user chats, the tool output formats in [BankingToolSet](file:///home/nickc/AndroidStudioProjects/InvestecBankingWidget/app/src/main/java/com/example/data/ai/BankingToolSet.kt) have been designed to return human-readable summaries.

Additionally, to prevent leaks across profiles, all tools are scoped to the **selected active profile** in the application interface.

Below are the conversational suggestions and how they are mapped in the codebase.

---

## 1. Account Listing & Balances (`getAllAccounts`)
Instead of returning a JSON array of database entities, the tool returns a clean, bulleted text list explaining all accounts under the **active profile** only.

* **Raw API Output (Internal)**:
  ```json
  [{"accountId":"1122","accountName":"Savings Account","availableBalance":12000.0,"currency":"ZAR","profileId":"prof_personal"}]
  ```
* **Conversational Suggestion (Returned by Tool to LLM)**:
  > *You have the following accounts under the active profile:*
  > *- Savings Account (Savings): (Account ID: 1122), Account No: 10010099, Available Balance: ZAR 12000.0 (Current Balance: ZAR 12000.0)*

---

## 2. Recent Transactions (`getRecentTransactions`)
Constructs a descriptive list of transactions for a specific account or across all accounts. The tool verifies that the requested account(s) belong to the **active profile** before displaying results.

* **Single Account Mode**:
  * **Raw API Output (Internal)**:
    ```json
    [{"type":"DEBIT","amount":450.0,"description":"Woolworths","transactionDate":"2026-06-12"}]
    ```
  * **Conversational Suggestion (Returned by Tool to LLM)**:
    > *The 5 most recent transactions for your account 'Savings Account' are:*
    > *1. DEBIT of ZAR 450.0 on 2026-06-12: 'Woolworths' [Type: Purchase, Status: Posted]*

* **All Accounts Mode (`accountId` set to `"ALL"`)**:
  * **Conversational Suggestion (Returned by Tool to LLM)**:
    > *The 5 most recent transactions across all your accounts are:*
    > *1. [Account: Savings Account] DEBIT of ZAR 450.0 on 2026-06-12: 'Woolworths' [Type: Purchase, Status: Posted]*

* **Out-of-Profile Error**:
  > *"Error: Account 'credit_card_99' was not found or does not belong to the active profile."*

---

## 3. All Transactions (`getAllTransactions`)
Constructs a descriptive list of all transactions stored in the database for a specific account or across all accounts. The tool verifies that the requested account(s) belong to the **active profile** before displaying results.

* **Single Account Mode**:
  * **Conversational Suggestion (Returned by Tool to LLM)**:
    > *All transactions for your account 'Savings Account' are:*
    > *1. DEBIT of ZAR 450.0 on 2026-06-12: 'Woolworths' [Type: Purchase, Status: Posted]*
    > *2. CREDIT of ZAR 1200.0 on 2026-06-10: 'Salary' [Type: Salary, Status: Posted]*

* **All Accounts Mode (`accountId` set to `"ALL"`)**:
  * **Conversational Suggestion (Returned by Tool to LLM)**:
    > *All transactions across all your accounts are:*
    > *1. [Account: Savings Account] DEBIT of ZAR 450.0 on 2026-06-12: 'Woolworths' [Type: Purchase, Status: Posted]*
    > *2. [Account: Credit Card] CREDIT of ZAR 1200.0 on 2026-06-10: 'Cash Deposit' [Type: Cash, Status: Posted]*

* **Out-of-Profile Error**:
  > *"Error: Account 'credit_card_99' was not found or does not belong to the active profile."*

---

## 4. Beneficiary Payments (`payBeneficiary`)
Performs a local database lookup to map IDs to readable names and describes the status conversationally. Validates that the payment source account belongs to the **active profile**.

* **Raw API Output (Internal)**:
  ```json
  {"paymentList": [{"paymentId": "pay_9876", "status": "Completed", "message": "Success"}]}
  ```
* **Conversational Suggestion (Returned by Tool to LLM)**:
  > *"Success: I have successfully paid R500.00 to beneficiary 'Bob Miller' from your account 'Private Savings' with reference 'dinner'. Status: Completed. Reference ID: pay_9876."*
* **Out-of-Profile Error**:
  > *"Error: Source account 'credit_card_99' was not found or does not belong to the active profile."*

---

## 5. Inter-Account Transfers (`transferFunds`)
Resolves account IDs to actual user-friendly display names. Validates that both the source and target accounts belong to the **active profile**.

* **Conversational Suggestion (Returned by Tool to LLM)**:
  > *"Success: R150.00 has been transferred from your account 'Transactional Account' to your account 'Private Savings' with reference 'monthly savings'. Status: Completed. Transfer ID: tx_2341."*
* **Out-of-Profile Error**:
  > *"Error: Source account 'credit_card_99' was not found or does not belong to the active profile."*

---

## 6. Pre-Approved Beneficiaries (`getBeneficiaries`)
Handles empty lists gracefully by returning a helpful call-to-action instead of an empty JSON array `[]`.

* **Conversational Suggestion**:
  > *"You don't have any pre-approved beneficiaries saved on your profile yet. You can add them via your online banking portal, and then synchronize data."*

---

## 7. Debit/Credit Cards (`getCards`)
Catches missing cards and provides friendly guidance instead of returning an empty array `[]`.

* **Conversational Suggestion**:
  > *"There are no active debit or credit cards linked to your profile."*

---

## How it Improves the Chat Experience
1. **Natural Dialogue**: Because the tool output is structured in a clear, descriptive sentence, the LLM naturally mimics that tone and replies conversationally to the user.
2. **Strict Profile Isolation**: If the user switches to a "Joint Account" profile in the app, the AI agent is instantly restricted to that profile's accounts only, preventing cross-profile data leakage in the chat interface.
3. **No Developer Jargon**: The chat remains clear of raw status codes (`HTTP 400`), transaction lists, or ID strings.
4. **Clean Formatting**: All raw tools output conversational, bullet-pointed lists, and the agent avoids using technical markdown structures (such as tables with `|:---|` dividers) or database notation.
