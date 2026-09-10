# =========================================================
# RDS PostgreSQL : base de données managée pour DemoBank
# =========================================================

# Récupère les sous-réseaux du VPC par défaut (pour placer la RDS)
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# Groupe de sous-réseaux : indique à RDS où elle peut vivre
resource "aws_db_subnet_group" "demobank" {
  name       = "demobank-db-subnet-group"
  subnet_ids = data.aws_subnets.default.ids
}

# Security group : n'autorise QUE les EC2 à joindre la base (port 5432)
resource "aws_security_group" "rds_sg" {
  name        = "demobank-rds-sg"
  description = "Autorise l acces PostgreSQL depuis les instances EC2 uniquement"

  ingress {
    description     = "PostgreSQL depuis le security group des EC2"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.web_sg.id]
  }

  egress {
    description = "Autorise tout le trafic sortant"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# L'instance RDS PostgreSQL
resource "aws_db_instance" "demobank" {
  identifier     = "demobank-db"
  engine         = "postgres"
  engine_version = "15"
  instance_class = "db.t3.micro"          # free tier

  allocated_storage = 20                   # 20 Go (minimum, free tier)
  storage_encrypted = true                 # chiffrement au repos (ton reflexe cyber)

  db_name  = var.postgres_db
  username = var.postgres_user
  password = var.postgres_password

  db_subnet_group_name   = aws_db_subnet_group.demobank.name
  vpc_security_group_ids = [aws_security_group.rds_sg.id]

  multi_az            = false              # Single-AZ (passer a true plus tard pour la HA)
  skip_final_snapshot = true               # pas de snapshot a la destruction (demo)
  publicly_accessible = false              # jamais accessible depuis Internet
}

# Output : l'adresse de connexion a la base
output "rds_endpoint" {
  value = aws_db_instance.demobank.address
}