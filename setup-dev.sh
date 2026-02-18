#!/bin/bash
# setup-dev.sh - Script de configuración para desarrollo

echo "🚀 SESA - Script de Configuración Desarrollo"
echo "=============================================="
echo ""

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Función para imprimir con color
print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Verificar pre-requisitos
echo "📋 Verificando pre-requisitos..."
echo ""

# Java
if ! command -v java &> /dev/null; then
    print_error "Java no está instalado. Instala JDK 17+"
    exit 1
else
    JAVA_VERSION=$(java -version 2>&1 | head -1)
    print_status "Java instalado: $JAVA_VERSION"
fi

# Maven o usar mvnw
if [ ! -f "sesa-backend/mvnw" ]; then
    print_error "Maven wrapper no encontrado en sesa-backend/mvnw"
    exit 1
fi
print_status "Maven wrapper encontrado"

# Node.js
if ! command -v node &> /dev/null; then
    print_error "Node.js no está instalado. Instala Node 18+"
    exit 1
else
    NODE_VERSION=$(node -v)
    print_status "Node.js instalado: $NODE_VERSION"
fi

# PostgreSQL
if ! command -v psql &> /dev/null; then
    print_warning "PostgreSQL no está en el PATH. Asegúrate de que esté corriendo en localhost:5432"
else
    print_status "PostgreSQL encontrado"
fi

echo ""
echo "🔧 Configurando Backend..."
echo ""

# Backend - Instalar dependencias
cd sesa-backend

print_status "Limpiando compilaciones anteriores..."
./mvnw clean -q

print_status "Descargando dependencias de Maven..."
./mvnw dependency:resolve -q

cd ..

echo ""
echo "🔧 Configurando Frontend..."
echo ""

# Frontend - Instalar dependencias
cd sesa-salud

print_status "Instalando dependencias npm..."
npm install --legacy-peer-deps

cd ..

echo ""
echo "🗄️  Configurando Base de Datos..."
echo ""

# Verificar si PostgreSQL está corriendo
if ! timeout 2 bash -c 'echo > /dev/tcp/localhost/5432' 2>/dev/null; then
    print_warning "PostgreSQL no está corriendo en localhost:5432"
    print_warning "Instala PostgreSQL o inicia el servicio manualmente"
else
    print_status "PostgreSQL verificado en localhost:5432"
    
    # Crear base de datos si no existe
    echo "Creando base de datos SESA..."
    psql -U postgres -h localhost -tc "SELECT 1 FROM pg_database WHERE datname = 'sesa_db'" | grep -q 1 || \
    psql -U postgres -h localhost -c "CREATE DATABASE sesa_db;" 2>/dev/null || \
    print_warning "No se pudo crear BD automáticamente. Considere crear manualmente."
fi

echo ""
echo "✨ Configuración completada!"
echo ""
echo "📌 Próximos pasos:"
echo ""
echo "1. Terminal 1 - Iniciar Backend:"
echo "   cd sesa-backend"
echo "   ./mvnw spring-boot:run -Dspring-boot.run.arguments=\"--spring.profiles.active=dev\""
echo ""
echo "2. Terminal 2 - Iniciar Frontend:"
echo "   cd sesa-salud"
echo "   npm start"
echo ""
echo "3. Acceder a la aplicación:"
echo "   Frontend: http://localhost:4200"
echo "   Backend:  http://localhost:8080/api"
echo ""
echo "4. Credenciales de prueba:"
echo "   Email: admin@sesa.com"
echo "   Password: admin123"
echo ""
print_warning "Nota: Asegúrate de que PostgreSQL esté corriendo antes de iniciar el backend"
echo ""
