# Groupe de sécurité du serveur web : HTTP + SSH entrants, tout sortant.
resource "aws_security_group" "web_sg" {
  name = "aws-ha-webapp-sg"

  # Entrée : autoriser le trafic web (port 80) depuis Internet
  ingress {
    description = "Autorise le trafic HTTP entrant depuis Internet"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  ingress {
    description = "Autorise le trafic HTTP entrant depuis Internet"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Sortie : autoriser tout le trafic sortant (pour télécharger Docker, les images ECR, etc.)
  egress {
	description = "Autorise tout le trafic sortant (telechargement images et paquets)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}