def log_constraint_error(description, context, fatal=False):
    """
    Logs errors in the required format and exits if fatal.
    
    Args:
        message: The main error message/type
        description: Detailed error description
        context: File name (if fatal) or constraint type (if non-fatal)
        fatal: If True, treats as fatal error and exits program
    """
    if fatal:
        print(f"ERROR: Fatal error - File {context} - {description}")
        #exit system code here
    else:
        print(f"ERROR: {context}: {description}")
