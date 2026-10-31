terraform {
  required_version = ">= 1.7, < 2.0"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 6.0" }
  }
}
provider "aws" { region = var.region }
resource "aws_db_subnet_group" "platform" {
  name       = "${var.name}-private"
  subnet_ids = var.private_subnet_ids
}
resource "aws_db_instance" "platform" {
  identifier                      = var.name
  engine                          = "postgres"
  instance_class                  = var.instance_class
  allocated_storage               = 30
  max_allocated_storage           = 100
  storage_type                    = "gp3"
  storage_encrypted               = true
  kms_key_id                      = var.kms_key_arn
  db_name                         = "agents"
  username                        = "platform_owner"
  manage_master_user_password     = true
  db_subnet_group_name            = aws_db_subnet_group.platform.name
  vpc_security_group_ids          = var.database_security_group_ids
  publicly_accessible             = false
  multi_az                        = true
  backup_retention_period         = 14
  deletion_protection             = true
  skip_final_snapshot             = false
  final_snapshot_identifier       = "${var.name}-final"
  auto_minor_version_upgrade      = true
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
  copy_tags_to_snapshot           = true
  tags                            = { Application = var.name, ManagedBy = "Terraform" }
}
output "database_endpoint" { value = aws_db_instance.platform.endpoint }
output "master_secret_arn" { value = aws_db_instance.platform.master_user_secret[0].secret_arn }
