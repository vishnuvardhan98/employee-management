/** TypeScript mirrors of the Spring Boot DTOs. */

export type Role = 'ADMIN' | 'USER';

export interface UserInfo {
  username: string;
  fullName: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
  user: UserInfo;
}

export type EmployeeStatus = 'ACTIVE' | 'ON_LEAVE' | 'INACTIVE';

export const STATUSES: readonly EmployeeStatus[] = ['ACTIVE', 'ON_LEAVE', 'INACTIVE'];

export const STATUS_LABELS: Record<EmployeeStatus, string> = {
  ACTIVE: 'Active',
  ON_LEAVE: 'On leave',
  INACTIVE: 'Inactive',
};

export interface Employee {
  id: number;
  name: string;
  email: string;
  phone: string;
  title: string;
  departmentId: number;
  departmentName: string;
  status: EmployeeStatus;
  joinedOn: string; // yyyy-MM-dd
}

export type EmployeeRequest = Omit<Employee, 'id' | 'departmentName'>;

export interface Department {
  id: number;
  name: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type SortDirection = 'asc' | 'desc';

/** Sort keys accepted by the API (whitelisted server-side). */
export type EmployeeSortKey = 'name' | 'email' | 'title' | 'department' | 'status' | 'joined';

export interface PageQuery {
  page: number;
  size: number;
  sort: EmployeeSortKey;
  direction: SortDirection;
}

export interface EmployeeStats {
  total: number;
  active: number;
  onLeave: number;
  inactive: number;
  departments: number;
}

export interface CategoryCount {
  key: string;
  count: number;
}

export interface MonthCount {
  year: number;
  month: number;
  count: number;
  label: string;
}

export interface ReportSummary {
  from: string;
  to: string;
  total: number;
  byStatus: CategoryCount[];
  byDepartment: CategoryCount[];
  byJoinMonth: MonthCount[];
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}

export const emptyPage = <T>(size = 10): Page<T> => ({ content: [], page: 0, size, totalElements: 0, totalPages: 0 });
