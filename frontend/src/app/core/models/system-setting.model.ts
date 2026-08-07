export interface SystemSetting {
  id: string;
  settingKey: string;
  settingValue: string;
  description: string | null;
}

export interface SystemSettingUpdateRequest {
  value: string;
}
