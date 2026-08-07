export interface PipelineStage {
  id: string;
  pipelineId: string;
  name: string;
  displayOrder: number;
  defaultProbability: number | null;
  slaDays: number | null;
  color: string | null;
  requiresLossReason: boolean;
}

export interface PipelineStageRequest {
  name: string;
  displayOrder?: number | null;
  defaultProbability?: number | null;
  slaDays?: number | null;
  color?: string | null;
  requiresLossReason?: boolean;
}

export interface Pipeline {
  id: string;
  name: string;
  businessType: string | null;
  active: boolean;
  stages: PipelineStage[];
}

export interface PipelineRequest {
  name: string;
  businessType?: string | null;
  active?: boolean;
}
