locals {
  ecs_cluster_id  = aws_ecs_cluster.ecs-cluster.id
  ecs_task_sg_id = aws_security_group.ecs-tasks-sg.id
  private_subnets = split(",", data.aws_ssm_parameter.deductions_private_private_subnets.value)
}

resource "aws_ecs_service" "ecs-service" {
  name            = "${var.environment}-${var.component_name}"
  cluster         = local.ecs_cluster_id
  task_definition = aws_ecs_task_definition.task.arn
  desired_count   = var.service_desired_count
  launch_type     = "FARGATE"

  lifecycle {
    ignore_changes = [desired_count]
  }

  network_configuration {
    security_groups = [local.ecs_task_sg_id]
    subnets         = local.private_subnets
  }
}

resource "aws_appautoscaling_target" "ecs_target" {
  count = var.environment == "dev" ? 1 : 0
  max_capacity       = 2
  min_capacity       = 0
  resource_id = "service/${aws_ecs_cluster.ecs-cluster.name}/${aws_ecs_service.ecs-service.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "ecs_policy" {
  count = var.environment == "dev" ? 1 : 0
  name               = "scale-down"
  policy_type        = "StepScaling"
  resource_id        = aws_appautoscaling_target.ecs_target[0].resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target[0].scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target[0].service_namespace

  step_scaling_policy_configuration {
    adjustment_type         = "ChangeInCapacity"
    cooldown                = 60
    metric_aggregation_type = "Maximum"

    step_adjustment {
      metric_interval_upper_bound = 0
      scaling_adjustment          = -1
    }
  }
}

resource "aws_ecs_cluster" "ecs-cluster" {
  name = "${var.environment}-${var.component_name}-ecs-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_ssm_parameter" "ecs-cluster-name" {
  name  = "/repo/${var.environment}/output/${var.component_name}/suspension-service-ecs-cluster-name"
  type  = "String"
  value = aws_ecs_cluster.ecs-cluster.name
}
