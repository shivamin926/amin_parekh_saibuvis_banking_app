def write_new_current_accounts(accounts, file_path):
    """
    Writes Current Bank Accounts File with strict validation
    Format: NNNNN AAAAAAAAAAAAAAAAAAAA S PPPPPPPP TT
    Where TT is account plan (SP or NP)
    """
    with open(file_path, 'w') as file:
        for acc in accounts:
            # Validate account number
            if not isinstance(acc['account_number'], str) or not acc['account_number'].isdigit():
                raise ValueError(f"Account number must be numeric string, got {acc['account_number']}")
            if len(acc['account_number']) > 5:
                raise ValueError(f"Account number exceeds 5 digits: {acc['account_number']}")

            # Validate name
            if len(acc['name']) > 20:
                raise ValueError(f"Account name exceeds 20 characters: {acc['name']}")

            # Validate status
            if acc['status'] not in ('A', 'D'):
                raise ValueError(f"Invalid status '{acc['status']}'. Must be 'A' or 'D'")

            # Validate balance with explicit negative check
            if not isinstance(acc['balance'], (int, float)):
                raise ValueError(f"Balance must be numeric, got {type(acc['balance'])}")
            if acc['balance'] < 0:
                raise ValueError(f"Negative balance detected: {acc['balance']}")
            if acc['balance'] > 99999.99:
                raise ValueError(f"Balance exceeds maximum $99999.99: {acc['balance']}")

            # Validate plan type
            plan = acc.get('plan', 'NP')
            if plan not in ('SP', 'NP'):
                raise ValueError(f"Invalid plan type '{plan}'. Must be SP or NP")

            # Format fields
            acc_num = acc['account_number'].zfill(5)
            name = acc['name'].ljust(20)[:20]
            balance = f"{acc['balance']:08.2f}"

            # Write line (37 chars + plan type = 39 chars total)
            file.write(f"{acc_num} {name} {acc['status']} {balance} {plan}\n")
        
        # Add END_OF_FILE marker
        file.write("00000 END_OF_FILE          A 00000.00 NP\n")