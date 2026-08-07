export interface Holiday {
  id: string;
  holidayDate: string;
  name: string;
  national: boolean;
  active: boolean;
}

export interface HolidayRequest {
  holidayDate: string;
  name: string;
  national?: boolean;
  active?: boolean;
}
