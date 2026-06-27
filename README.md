# Backend_Proyecto_JavaEE

Backend Java EE para el chat empresarial.

Incluye Servlets, capa DAO/Service, integracion con ECR y despliegue automatizable en EC2 mediante GitHub Actions.

## Estado AWS actual

- EC2 publica: `i-0105066ec62ac404a` (`nexolab-prod-app-ec2`) con EIP `44.210.180.79`.
- SG_EC2: HTTP 80, HTTPS 443 y SSH 22 restringido por IP.
- SG_RDS: PostgreSQL 5432 solo desde SG_EC2.
- RDS `rds-postgres`: privada, sin acceso publico.

