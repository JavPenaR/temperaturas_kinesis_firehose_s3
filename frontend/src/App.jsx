import { useState } from 'react'

export default function App() {
  const [sensorId, setSensorId] = useState('sala-1')
  const [temperatura, setTemperatura] = useState('22.5')
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function enviar(event) {
    event.preventDefault()
    setError('')
    setResultado(null)
    setEnviando(true)
    try {
      const response = await fetch('/api/lecturas', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sensorId, temperatura: Number(temperatura) }),
      })
      const data = await response.json()
      if (!response.ok) throw new Error(data.error || 'Error al enviar la lectura')
      setResultado(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <main className="container">
      <p className="eyebrow">Laboratorio de streaming</p>
      <h1>Lecturas de temperatura</h1>
      <p>Envía una lectura al stream de Kinesis mediante la API de Spring Boot.</p>

      <form onSubmit={enviar}>
        <label htmlFor="sensor">ID del sensor</label>
        <input id="sensor" value={sensorId} onChange={e => setSensorId(e.target.value)}
          pattern="[A-Za-z0-9_-]{1,64}" title="1 a 64 letras, números, guiones o guiones bajos" required />

        <label htmlFor="temperatura">Temperatura (°C)</label>
        <input id="temperatura" type="number" min="-100" max="200" step="0.1"
          value={temperatura} onChange={e => setTemperatura(e.target.value)} required />

        <button type="submit" disabled={enviando}>{enviando ? 'Enviando…' : 'Enviar a Kinesis'}</button>
      </form>

      {error && <div className="message error" role="alert">{error}</div>}
      {resultado && <div className="message success" role="status">
        <strong>Registro aceptado por Kinesis</strong>
        <dl>
          <dt>Evento</dt><dd>{resultado.eventoId}</dd>
          <dt>Shard</dt><dd>{resultado.shardId}</dd>
          <dt>Secuencia</dt><dd>{resultado.sequenceNumber}</dd>
        </dl>
      </div>}
    </main>
  )
}
