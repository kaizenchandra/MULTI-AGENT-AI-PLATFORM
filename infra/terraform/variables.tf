variable "region" {
  type        = string
  description = "Approved AWS region"
}
variable "name" {
  type    = string
  default = "kaizen-agents"
}
variable "private_subnet_ids" {
  type = list(string)
  validation {
    condition     = length(var.private_subnet_ids) >= 2
    error_message = "Use private subnets in at least two availability zones."
  }
}
variable "database_security_group_ids" {
  type = list(string)
  validation {
    condition     = length(var.database_security_group_ids) > 0
    error_message = "Provide security groups allowing only approved workload identities."
  }
}
variable "kms_key_arn" {
  type        = string
  description = "Approved customer-managed KMS key for the database"
}
variable "instance_class" {
  type    = string
  default = "db.t4g.medium"
}
