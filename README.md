# Laboratorio 2: Kinesis Data Streams → Data Firehose → S3

Esta es la continuación del proyecto de temperaturas. El formulario React y el backend Spring Boot siguen publicando en el stream `lab-lecturas`. **Amazon Data Firehose lee ese stream y entrega archivos JSON Lines a S3**; el backend no necesita otro envío ni credenciales de S3.

```text
React → Spring Boot → Kinesis Data Streams (lab-lecturas)
                              ↓
                   Amazon Data Firehose (lab-lecturas-s3)
                              ↓
                     S3 (lecturas/año/mes/día/hora/)
```

## Requisitos

- Tener acceso a una cuenta AWS para crear el stream `lab-lecturas`. Si ya lo creaste en la primera parte, verifica que siga en estado **Active** y continúa con Firehose.
- Tener la misma región para el stream, Firehose y el bucket S3.
- Permisos de la cuenta para Firehose, S3 e IAM. En **AWS Academy Learner Lab**, comprueba el botón **Readme** del laboratorio: los servicios y permisos disponibles dependen del curso. No hay que guardar claves AWS dentro de este proyecto.

## Paso 1: crear el stream de Kinesis Data Streams

Si el stream `lab-lecturas` ya existe y está **Active**, puedes pasar al siguiente apartado. Si no existe, créalo antes de configurar Firehose:

1. En **AWS Academy → Learner Lab**, inicia el laboratorio con **Start Lab** y abre la consola desde el botón **AWS**. En una cuenta AWS propia, entra directamente a la consola.
2. En el selector superior de región, elige la región que usarás en toda la práctica; el ejemplo del proyecto utiliza **US East (N. Virginia), `us-east-1`**. Anótala para usarla también en S3, Firehose y la configuración del backend.
3. Busca **Kinesis** en la consola y abre **Amazon Kinesis → Data streams**. Selecciona **Create data stream**.
4. En **Data stream name**, escribe exactamente **`lab-lecturas`**. El backend publica en ese nombre de manera predeterminada.
5. En **Capacity mode**, selecciona **Provisioned** y configura **1 shard** para este ejercicio de bajo tráfico. Conserva las demás opciones predeterminadas. También es posible usar **On-demand** si el laboratorio lo requiere; en ese caso no se indica el número de shards.
6. Selecciona **Create Kinesis stream**. Espera a que el estado cambie de **Creating** a **Active** y abre el stream para comprobar su nombre y región.

Si aparece **Access denied** o Kinesis no está disponible, revisa el **Readme** de Learner Lab y consulta al docente sobre los permisos del curso. No crees otro stream con un nombre distinto sin actualizar `KINESIS_STREAM_NAME` en el backend y el parámetro `KinesisStreamName` de CloudFormation.

Fuente: [crear un stream desde la consola AWS](https://docs.aws.amazon.com/streams/latest/dev/how-do-i-create-a-stream.html).

## Paso 2, opción A: crear Firehose y S3 con CloudFormation

La plantilla [`infra/firehose-s3.json`](infra/firehose-s3.json) crea un bucket privado con cifrado SSE-S3, un rol que permite a Firehose leer el stream y escribir en ese bucket, y un flujo de Firehose. **No crea ni modifica el stream existente.** El nombre del bucket debe ser único en S3 y estar en minúsculas.

En PowerShell, con AWS CLI configurado y situado en la raíz del proyecto:

```powershell
$env:AWS_PROFILE = "lab-kinesis"
$env:AWS_REGION = "us-east-1"  # usa la región real del stream
$bucket = "lab-lecturas-<identificador-unico-en-minusculas>"
aws cloudformation deploy `
  --template-file infra/firehose-s3.json `
  --stack-name lab-lecturas-firehose-s3 `
  --capabilities CAPABILITY_IAM `
  --parameter-overrides KinesisStreamName=lab-lecturas BucketName=$bucket
```

Reemplaza el valor de `$bucket` antes de ejecutar. Si el laboratorio no permite `iam:CreateRole` o `iam:PassRole`, la creación fallará; solicita un rol permitido al docente o usa la opción de consola si tu cuenta permite que Firehose cree su rol.

Para ver las salidas de la pila:

```powershell
aws cloudformation describe-stacks --stack-name lab-lecturas-firehose-s3 --query "Stacks[0].Outputs"
```

## Paso 2, opción B: crear Firehose y S3 desde la consola AWS

1. Abre **Amazon S3 → Create bucket**. Asigna un nombre único, usa la **misma región** que `lab-lecturas`, mantén el bloqueo de acceso público y el cifrado predeterminado. Crea el bucket.
2. Abre **Amazon Data Firehose → Create Firehose stream** (el nombre exacto del botón puede variar en la consola). Elige **Amazon Kinesis Data Streams** como origen y **Amazon S3** como destino.
3. Selecciona el stream existente `lab-lecturas` y el bucket del paso 1. Nombra el flujo `lab-lecturas-s3`.
4. Deja desactivadas la transformación Lambda, la conversión de formato, el particionado dinámico y la compresión. Activa **Append delimiter to record** o **New line delimiter**, para que cada evento JSON ocupe una línea.
5. Para el destino S3, usa el prefijo `lecturas/` y un buffer de **1 MiB o 60 segundos**. El tiempo es orientativo: la entrega puede tardar más.
6. En los permisos, permite que la consola cree un rol para Firehose o selecciona uno ya autorizado. El rol necesita leer `lab-lecturas` (`DescribeStream`, `GetShardIterator`, `GetRecords`, `ListShards`) y escribir objetos en el bucket. Crea el flujo y espera a que figure como **Active**.

Si el rol existente de Academy no tiene esos permisos, la consola puede permitir crear el flujo pero luego mostrar errores de entrega. Revisa las métricas de Firehose y el rol asignado.

## Paso 3: enviar datos y comprobar S3

**Crea Firehose antes de enviar nuevas lecturas:** cuando usa Kinesis Data Streams como origen, empieza a leer desde la posición `LATEST`, por lo que las lecturas anteriores a su creación no sirven para esta prueba.

Desde VS Code puedes usar **Run and Debug → Iniciar Kinesis Lab** para el backend y `npm run dev` en `frontend`. O desde terminal:

```powershell
$env:AWS_PROFILE = "lab-kinesis"
$env:AWS_REGION = "us-east-1"
$env:KINESIS_STREAM_NAME = "lab-lecturas"
cd backend
.\mvnw.cmd spring-boot:run
```

En otra terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Abre `http://localhost:5173` y envía varias temperaturas. El `sequenceNumber` en la página confirma que **Kinesis aceptó** el registro; aún no confirma que esté en S3. Espera al menos un intervalo de buffer y abre **S3 → tu bucket → lecturas/**. Descarga un objeto y comprueba líneas como esta:

```json
{"eventoId":"...","sensorId":"sala-1","temperatura":22.5,"registradoEn":"2026-11-05T15:00:00Z"}
```

`registradoEn` es UTC y lo genera el backend. Las carpetas de S3 se basan en la hora de entrega de Firehose, también en UTC, así que cerca de un cambio de hora pueden diferir de `registradoEn`. Un objeto S3 puede contener varias lecturas. La entrega puede repetirse; `eventoId` permite deduplicar si más adelante se analizan los archivos.

Si no aparecen objetos, confirma que el flujo está **Active**, que el bucket y el stream están en la misma región y que enviaste datos **después** de crear el flujo. En las métricas de Firehose comprueba `DataReadFromKinesisStream.Records` y los errores de entrega a S3. Revisa también el prefijo `errores/` y los permisos del rol.

## Cierre del laboratorio

Elimina primero el flujo de Firehose. Si usaste CloudFormation, elimina la pila después. La plantilla conserva el bucket al borrar la pila para evitar perder datos por accidente: **vacía y elimina manualmente el bucket** cuando ya no necesites los archivos. Elimina el stream `lab-lecturas` al finalizar la práctica. Estos servicios pueden consumir presupuesto mientras existan.

## Fuentes AWS

- [Kinesis Data Streams como origen de Firehose](https://docs.aws.amazon.com/firehose/latest/dev/writing-with-kinesis-streams.html)
- [Entrega y delimitadores en S3](https://docs.aws.amazon.com/firehose/latest/dev/basic-deliver.html)
- [Permisos IAM de Firehose para S3 y Kinesis](https://docs.aws.amazon.com/firehose/latest/dev/controlling-access.html)
- [Recurso CloudFormation de Firehose](https://docs.aws.amazon.com/AWSCloudFormation/latest/TemplateReference/aws-resource-kinesisfirehose-deliverystream.html)
