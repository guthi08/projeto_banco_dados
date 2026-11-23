-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: mydb
-- ------------------------------------------------------
-- Server version	8.0.46-0ubuntu0.24.04.4

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `Auditoria`
--

DROP TABLE IF EXISTS `Auditoria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Auditoria` (
  `codigo` int NOT NULL,
  `categoria` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nivel_gravidade` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  `local_operacao_codigo` int NOT NULL,
  `Operacao_voo_Codigo` int NOT NULL,
  PRIMARY KEY (`codigo`,`Operacao_voo_Codigo`),
  UNIQUE KEY `codigo_UNIQUE` (`codigo`),
  KEY `fk_Auditoria_Operacao_voo1_idx` (`Operacao_voo_Codigo`),
  CONSTRAINT `fk_Auditoria_Operacao_voo1` FOREIGN KEY (`Operacao_voo_Codigo`) REFERENCES `Operacao_voo` (`Codigo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Auditoria`
--

LOCK TABLES `Auditoria` WRITE;
/*!40000 ALTER TABLE `Auditoria` DISABLE KEYS */;
/*!40000 ALTER TABLE `Auditoria` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Empresa_Ope`
--

DROP TABLE IF EXISTS `Empresa_Ope`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Empresa_Ope` (
  `cnpj` char(18) COLLATE utf8mb4_unicode_ci NOT NULL,
  `razao_social` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  `setor_atuacao` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`cnpj`),
  UNIQUE KEY `cnpj_UNIQUE` (`cnpj`),
  UNIQUE KEY `razao_social_UNIQUE` (`razao_social`),
  UNIQUE KEY `email_UNIQUE` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Empresa_Ope`
--

LOCK TABLES `Empresa_Ope` WRITE;
/*!40000 ALTER TABLE `Empresa_Ope` DISABLE KEYS */;
INSERT INTO `Empresa_Ope` VALUES ('10101010000110','CineDrone Produções','Mídia','producao@cinedrone.com'),('11111111000111','AgroDrone Tech','Agricultura','contato@agrodrone.com'),('22222222000122','Skyline Mapeamentos','Geodésia','suporte@skyline.com'),('33333333000133','AeroInspeções Brasil','Energia','atendimento@aeroinspecao.com'),('44444444000144','Topografia Digital SP','Construção Civil','contato@topodigital.com'),('55555555000155','DroneRescue Resgates','Segurança','contato@dronerescue.com'),('66666666000166','Fotografia Aérea Sul','Entretenimento','contato@fotosul.com'),('77777777000177','Monitoramento Ambiental Verde','Meio Ambiente','verde@ambientaldrone.com'),('88888888000188','Logística Express Drones','Logística','logistica@expressdrone.com'),('99999999000199','Inspeção de Oleodutos S/A','Óleo e Gás','contato@oleodutrone.com');
/*!40000 ALTER TABLE `Empresa_Ope` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Operacao_voo`
--

DROP TABLE IF EXISTS `Operacao_voo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Operacao_voo` (
  `Codigo` int NOT NULL,
  `status_voo` enum('Agendado','Em andamento','Concluido','Cancelado') COLLATE utf8mb4_unicode_ci NOT NULL,
  `Obetivo_voo` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  `data_hora_planejada` date NOT NULL,
  `data_hora_real` date NOT NULL,
  `clientes_cpf_rg` char(14) COLLATE utf8mb4_unicode_ci NOT NULL,
  `clientes_Empresa_Ope_cnpj` char(18) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descricao_area` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo_local` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`Codigo`),
  UNIQUE KEY `Codigo_UNIQUE` (`Codigo`),
  KEY `fk_Operacao_voo_clientes1_idx` (`clientes_cpf_rg`,`clientes_Empresa_Ope_cnpj`),
  KEY `idx_status_voo` (`status_voo`),
  CONSTRAINT `fk_Operacao_voo_clientes1` FOREIGN KEY (`clientes_cpf_rg`, `clientes_Empresa_Ope_cnpj`) REFERENCES `clientes` (`cpf_rg`, `Empresa_Ope_cnpj`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Operacao_voo`
--

LOCK TABLES `Operacao_voo` WRITE;
/*!40000 ALTER TABLE `Operacao_voo` DISABLE KEYS */;
INSERT INTO `Operacao_voo` VALUES (1001,'Concluido','Pulverização de lavoura','2026-03-01','2026-03-01','12345678901234','11111111000111','Setor Norte da Fazenda','Rural'),(1002,'Concluido','Mapeamento topográfico urbano','2026-03-05','2026-03-05','23456789012345','22222222000122','Centro Cívico','Urbano'),(1003,'Em andamento','Inspeção de barragem','2026-03-26','2026-03-26','34567890123456','33333333000133','Setor de Turbinas','Industrial'),(1004,'Agendado','Levantamento planialtimétrico','2026-03-30','2026-03-30','45678901234567','44444444000144','Loteamento Nova Aurora','Urbano'),(1005,'Agendado','Busca de pessoa desaparecida','2026-03-27','2026-03-27','56789012345678','55555555000155','Região Serrana','Misto'),(1006,'Cancelado','Gravação de comercial','2026-02-10','2026-02-10','67890123456789','66666666000166','Orla da Praia','Urbano'),(1007,'Concluido','Monitoramento de desmatamento','2026-02-20','2026-02-20','78901234567890','77777777000177','Parque Estadual','Rural'),(1008,'Em andamento','Entrega de medicamentos','2026-03-26','2026-03-26','89012345678901','88888888000188','Rota Hospitalar','Urbano'),(1009,'Concluido','Inspeção de dutos de gás','2026-02-15','2026-02-15','90123456789012','99999999000199','Trecho KM 40','Industrial'),(1010,'Agendado','Filmagem aérea de longa-metragem','2026-04-02','2026-04-02','01234567890123','10101010000110','Estúdio Externo','Rural');
/*!40000 ALTER TABLE `Operacao_voo` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Operacao_voo_has_piloto`
--

DROP TABLE IF EXISTS `Operacao_voo_has_piloto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Operacao_voo_has_piloto` (
  `Operacao_voo_Codigo` int NOT NULL,
  `piloto_cpf` char(14) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`Operacao_voo_Codigo`,`piloto_cpf`),
  KEY `fk_Operacao_voo_has_piloto_piloto1_idx` (`piloto_cpf`),
  KEY `fk_Operacao_voo_has_piloto_Operacao_voo1_idx` (`Operacao_voo_Codigo`),
  CONSTRAINT `fk_Operacao_voo_has_piloto_Operacao_voo1` FOREIGN KEY (`Operacao_voo_Codigo`) REFERENCES `Operacao_voo` (`Codigo`),
  CONSTRAINT `fk_Operacao_voo_has_piloto_piloto1` FOREIGN KEY (`piloto_cpf`) REFERENCES `piloto` (`cpf`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Operacao_voo_has_piloto`
--

LOCK TABLES `Operacao_voo_has_piloto` WRITE;
/*!40000 ALTER TABLE `Operacao_voo_has_piloto` DISABLE KEYS */;
INSERT INTO `Operacao_voo_has_piloto` VALUES (1001,'11122233344'),(1002,'22233344455'),(1003,'33344455566'),(1004,'44455566677'),(1005,'55566677788');
/*!40000 ALTER TABLE `Operacao_voo_has_piloto` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `clientes`
--

DROP TABLE IF EXISTS `clientes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `clientes` (
  `cpf_rg` char(14) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nome` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Empresa_Ope_cnpj` char(18) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status_contrato` enum('Ativo','Inativo') COLLATE utf8mb4_unicode_ci NOT NULL,
  `data_` date NOT NULL,
  `rg` char(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`cpf_rg`,`Empresa_Ope_cnpj`),
  UNIQUE KEY `cpf_rg_UNIQUE` (`cpf_rg`),
  UNIQUE KEY `rg_UNIQUE` (`rg`),
  KEY `fk_clientes_Empresa_Ope1_idx` (`Empresa_Ope_cnpj`),
  KEY `idx_nome_cliente` (`nome`),
  CONSTRAINT `fk_clientes_Empresa_Ope1` FOREIGN KEY (`Empresa_Ope_cnpj`) REFERENCES `Empresa_Ope` (`cnpj`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `clientes`
--

LOCK TABLES `clientes` WRITE;
/*!40000 ALTER TABLE `clientes` DISABLE KEYS */;
INSERT INTO `clientes` VALUES ('01234567890123','Studio de Cinema Lumiere','10101010000110','Ativo','2026-03-22','MG01234567'),('12345678901234','Fazenda Boa Vista','11111111000111','Ativo','2026-01-10','MG12345678'),('23456789012345','Prefeitura Municipal de SP','22222222000122','Ativo','2026-02-15','SP23456789'),('34567890123456','Usina Hidrelétrica Itaipu','33333333000133','Ativo','2026-03-01','PR34567890'),('45678901234567','Construtora Engel Corp','44444444000144','Ativo','2026-03-12','RJ45678901'),('56789012345678','Defesa Civil Estadual','55555555000155','Ativo','2026-03-20','SC56789012'),('67890123456789','Produtora Filme Brasil','66666666000166','Inativo','2025-11-10','RS67890123'),('78901234567890','Instituto Ambiental Paranaense','77777777000177','Ativo','2026-01-25','PR78901234'),('89012345678901','Empresa de Logística Fast','88888888000188','Ativo','2026-02-05','SP89012345'),('90123456789012','Petrobrás Infraestrutura','99999999000199','Ativo','2026-01-18','RJ90123456');
/*!40000 ALTER TABLE `clientes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `drone`
--

DROP TABLE IF EXISTS `drone`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `drone` (
  `Codigo` int NOT NULL,
  `peso_max_decolagem` decimal(5,2) NOT NULL,
  `Num_serie` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  `modelo` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo_uso` varchar(25) COLLATE utf8mb4_unicode_ci NOT NULL,
  `fabricante` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  `data_validade_cadastro` date DEFAULT NULL,
  `Empresa_Ope_cnpj` char(18) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`Codigo`,`Empresa_Ope_cnpj`),
  KEY `fk_drone_Empresa_Ope1_idx` (`Empresa_Ope_cnpj`),
  KEY `idx_modelo_drone` (`modelo`),
  CONSTRAINT `fk_drone_Empresa_Ope1` FOREIGN KEY (`Empresa_Ope_cnpj`) REFERENCES `Empresa_Ope` (`cnpj`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `drone`
--

LOCK TABLES `drone` WRITE;
/*!40000 ALTER TABLE `drone` DISABLE KEYS */;
INSERT INTO `drone` VALUES (101,15.50,'SN-1001-AG','Agras T30','Agrícola','DJI','2027-12-31','11111111000111'),(102,3.20,'SN-1002-SK','Matrice 300','Mapeamento','DJI','2027-10-15','22222222000122'),(103,8.40,'SN-1003-AE','Inspector V2','Inspeção','Flyability','2026-08-20','33333333000133'),(104,2.50,'SN-1004-TP','Phantom 4 RTK','Topografia','DJI','2028-01-10','44444444000144'),(105,12.00,'SN-1005-RS','Rescuer Pro','Busca e Resgate','Autel','2027-05-05','55555555000155'),(106,1.80,'SN-1006-FS','Inspire 2','Cinematográfico','DJI','2026-11-30','66666666000166'),(107,5.00,'SN-1007-MA','EcoDrone X','Ambiental','Parrot','2027-09-12','77777777000177'),(108,25.00,'SN-1008-LG','CargoLift 500','Entregas','AeroDelivery','2028-03-25','88888888000188'),(109,10.10,'SN-1009-OL','PipeInspector','Industrial','Skydio','2027-07-07','99999999000199'),(110,4.00,'SN-1010-CN','CinemaLift 4K','Mídia','DJI','2028-06-18','10101010000110');
/*!40000 ALTER TABLE `drone` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `drone_has_Operacao_voo`
--

DROP TABLE IF EXISTS `drone_has_Operacao_voo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `drone_has_Operacao_voo` (
  `drone_Codigo` int NOT NULL,
  `drone_Empresa_Ope_cnpj` char(18) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Operacao_voo_Codigo` int NOT NULL,
  PRIMARY KEY (`drone_Codigo`,`drone_Empresa_Ope_cnpj`,`Operacao_voo_Codigo`),
  KEY `fk_drone_has_Operacao_voo_Operacao_voo1_idx` (`Operacao_voo_Codigo`),
  KEY `fk_drone_has_Operacao_voo_drone1_idx` (`drone_Codigo`,`drone_Empresa_Ope_cnpj`),
  CONSTRAINT `fk_drone_has_Operacao_voo_drone1` FOREIGN KEY (`drone_Codigo`, `drone_Empresa_Ope_cnpj`) REFERENCES `drone` (`Codigo`, `Empresa_Ope_cnpj`),
  CONSTRAINT `fk_drone_has_Operacao_voo_Operacao_voo1` FOREIGN KEY (`Operacao_voo_Codigo`) REFERENCES `Operacao_voo` (`Codigo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `drone_has_Operacao_voo`
--

LOCK TABLES `drone_has_Operacao_voo` WRITE;
/*!40000 ALTER TABLE `drone_has_Operacao_voo` DISABLE KEYS */;
INSERT INTO `drone_has_Operacao_voo` VALUES (101,'11111111000111',1001),(102,'22222222000122',1002),(103,'33333333000133',1003),(104,'44444444000144',1004),(105,'55555555000155',1005);
/*!40000 ALTER TABLE `drone_has_Operacao_voo` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `piloto`
--

DROP TABLE IF EXISTS `piloto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `piloto` (
  `cpf` char(14) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rg` char(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `emial` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  `validade_CMA` date NOT NULL,
  `num_registro_licenca` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nome` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `Empresa_Ope_cnpj` char(18) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`cpf`,`Empresa_Ope_cnpj`),
  UNIQUE KEY `cpf_UNIQUE` (`cpf`),
  UNIQUE KEY `rg_UNIQUE` (`rg`),
  UNIQUE KEY `emial_UNIQUE` (`emial`),
  KEY `fk_piloto_Empresa_Ope1_idx` (`Empresa_Ope_cnpj`),
  KEY `idx_nome_piloto` (`nome`),
  CONSTRAINT `fk_piloto_Empresa_Ope1` FOREIGN KEY (`Empresa_Ope_cnpj`) REFERENCES `Empresa_Ope` (`cnpj`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `piloto`
--

LOCK TABLES `piloto` WRITE;
/*!40000 ALTER TABLE `piloto` DISABLE KEYS */;
INSERT INTO `piloto` VALUES ('11122233344','1234567891','carlos.silva@agrodrone.com','2027-05-10','ANAC-98765','Carlos Silva','11111111000111'),('22233344455','2345678912','ana.souza@skyline.com','2027-08-20','ANAC-87654','Ana Souza','22222222000122'),('33344455566','3456789123','marcos.lima@aeroinspecao.com','2026-12-15','ANAC-76543','Marcos Lima','33333333000133'),('44455566677','4567891234','beatriz.alves@topodigital.com','2028-03-10','ANAC-65432','Beatriz Alves','44444444000144'),('55566677788','5678912345','lucas.rocha@dronerescue.com','2027-11-05','ANAC-54321','Lucas Rocha','55555555000155');
/*!40000 ALTER TABLE `piloto` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `telefone_cliente`
--

DROP TABLE IF EXISTS `telefone_cliente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `telefone_cliente` (
  `telefone_cliente` int NOT NULL,
  `clientes_cpf_rg` char(14) COLLATE utf8mb4_unicode_ci NOT NULL,
  `clientes_Empresa_Ope_cnpj` char(18) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`telefone_cliente`,`clientes_cpf_rg`,`clientes_Empresa_Ope_cnpj`),
  KEY `fk_telefone_clientes1_idx` (`clientes_cpf_rg`,`clientes_Empresa_Ope_cnpj`),
  CONSTRAINT `fk_telefone_clientes1` FOREIGN KEY (`clientes_cpf_rg`, `clientes_Empresa_Ope_cnpj`) REFERENCES `clientes` (`cpf_rg`, `Empresa_Ope_cnpj`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `telefone_cliente`
--

LOCK TABLES `telefone_cliente` WRITE;
/*!40000 ALTER TABLE `telefone_cliente` DISABLE KEYS */;
/*!40000 ALTER TABLE `telefone_cliente` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `telefone_empresa`
--

DROP TABLE IF EXISTS `telefone_empresa`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `telefone_empresa` (
  `telefone` char(11) COLLATE utf8mb4_unicode_ci NOT NULL,
  `Empresa_Ope_cnpj` char(18) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`telefone`,`Empresa_Ope_cnpj`),
  UNIQUE KEY `telefone_UNIQUE` (`telefone`),
  KEY `fk_table1_Empresa_Ope1_idx` (`Empresa_Ope_cnpj`),
  CONSTRAINT `fk_table1_Empresa_Ope1` FOREIGN KEY (`Empresa_Ope_cnpj`) REFERENCES `Empresa_Ope` (`cnpj`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `telefone_empresa`
--

LOCK TABLES `telefone_empresa` WRITE;
/*!40000 ALTER TABLE `telefone_empresa` DISABLE KEYS */;
INSERT INTO `telefone_empresa` VALUES ('11988880000','10101010000110'),('11988881111','11111111000111'),('11988882222','22222222000122'),('11988883333','33333333000133'),('11988884444','44444444000144'),('11988885555','55555555000155'),('11988886666','66666666000166'),('11988887777','77777777000177'),('11988888888','88888888000188'),('11988889999','99999999000199');
/*!40000 ALTER TABLE `telefone_empresa` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `telefone_piloto`
--

DROP TABLE IF EXISTS `telefone_piloto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `telefone_piloto` (
  `telefone_` char(11) COLLATE utf8mb4_unicode_ci NOT NULL,
  `piloto_cpf` char(14) COLLATE utf8mb4_unicode_ci NOT NULL,
  `piloto_Empresa_Ope_cnpj` char(18) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`telefone_`,`piloto_cpf`,`piloto_Empresa_Ope_cnpj`),
  KEY `fk_telefone_piloto1_idx` (`piloto_cpf`,`piloto_Empresa_Ope_cnpj`),
  CONSTRAINT `fk_telefone_piloto1` FOREIGN KEY (`piloto_cpf`, `piloto_Empresa_Ope_cnpj`) REFERENCES `piloto` (`cpf`, `Empresa_Ope_cnpj`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `telefone_piloto`
--

LOCK TABLES `telefone_piloto` WRITE;
/*!40000 ALTER TABLE `telefone_piloto` DISABLE KEYS */;
INSERT INTO `telefone_piloto` VALUES ('11977771111','11122233344','11111111000111'),('11977772222','22233344455','22222222000122'),('11977773333','33344455566','33333333000133'),('11977774444','44455566677','44444444000144'),('11977775555','55566677788','55555555000155');
/*!40000 ALTER TABLE `telefone_piloto` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-28 21:55:25
