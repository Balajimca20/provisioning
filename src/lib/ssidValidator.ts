/**
 * SsidValidator replicating com.royalenfield.provisioning.core.validation.SsidValidator
 * 
 * Rules:
 * 1. Must start with "RE_"
 * 2. Must be exactly 14 characters long (e.g., RE_LXHD_250925)
 * 3. Strips quotes automatically if entered as "SSID"
 * 4. Only contains uppercase alphanumeric and underscore characters
 */

export class SsidValidator {
  public static readonly REQUIRED_PREFIX = 'RE_';
  public static readonly REQUIRED_LENGTH = 14;

  /**
   * Normalizes an SSID by stripping leading and trailing whitespace & quotes.
   */
  public static sanitize(input: string): string {
    if (!input) return '';
    let cleaned = input.trim();
    if ((cleaned.startsWith('"') && cleaned.endsWith('"')) ||
        (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
      cleaned = cleaned.substring(1, cleaned.length - 1).trim();
    }
    return cleaned.toUpperCase();
  }

  /**
   * Returns a validation error message if invalid, or null if valid.
   */
  public static getValidationError(input: string): string | null {
    if (!input || input.trim().length === 0) {
      return 'SSID is required';
    }

    const sanitized = this.sanitize(input);

    if (!sanitized.startsWith(this.REQUIRED_PREFIX)) {
      return `SSID must start with "${this.REQUIRED_PREFIX}"`;
    }

    if (sanitized.length < this.REQUIRED_LENGTH) {
      return `SSID is too short (${sanitized.length}/${this.REQUIRED_LENGTH} chars). Format: RE_XXXX_XXXXXX`;
    }

    if (sanitized.length > this.REQUIRED_LENGTH) {
      return `SSID is too long (${sanitized.length}/${this.REQUIRED_LENGTH} chars). Format: RE_XXXX_XXXXXX`;
    }

    // Pattern: RE_ followed by 4 alphanumeric chars, underscore, 6 alphanumeric chars
    const pattern = /^RE_[A-Z0-9]{4}_[A-Z0-9]{6}$/;
    if (!pattern.test(sanitized)) {
      return 'Invalid format. Required structure: RE_XXXX_XXXXXX (e.g. RE_LXHD_250925)';
    }

    return null;
  }

  /**
   * Validates if string conforms to vehicle AP standard
   */
  public static isValidVehicleSsid(input: string): boolean {
    return this.getValidationError(input) === null;
  }
}
