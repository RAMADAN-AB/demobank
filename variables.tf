variable "postgres_db" {
  type    = string
  default = "demobank_db"
}

variable "postgres_user" {
  type    = string
  default = "demobank"
}

variable "postgres_password" {
  type      = string
  sensitive = true
}

variable "jwt_secret" {
  type      = string
  sensitive = true
}

variable "jwt_expiration" {
  type    = string
  default = "86400000"
}