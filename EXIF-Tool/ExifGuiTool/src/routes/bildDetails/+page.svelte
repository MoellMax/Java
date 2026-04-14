<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';


  let fileUrl = '';
  let filename = "";
  let exifData: Record<string, any> = {};

  // ######################################################################################

  let editMode = false;

    // editierbare Felder
  let form = {
    name: '',
    titel: '',
    betreff: '',
    autoren: '',
    copyright: '',
    kameraHersteller: '',
    kameraModell: '',
    objektivHersteller: '',
    objektivModell: '',
    latitude: 0 as number,
    longitude: 0 as number,
    address: '',
    fotoDateiGeo: ''
  };



  onMount(async () => {
    filename = new URLSearchParams(window.location.search).get('image') ?? '';
    if (!filename) goto('/');

    fileUrl = `http://localhost:8080/testapp-1.0/api/foto/image/${filename}`;

    const res = await fetch(
      `http://localhost:8080/testapp-1.0/api/foto/update-exif-by-name/${filename}`
    );
    exifData = await res.json();


    // Vorbelegen
    form.name = exifData['Exif IFD0']?.['Page Name'] || exifData['DB_Name'] || '';
    form.titel = exifData['Exif IFD0']?.['Image Description'] || exifData['DB_Titel'] || '';
    form.betreff = exifData['Exif IFD0']?.['Document Name'] || exifData['DB_Betreff'] || '';
    form.autoren = exifData['Exif IFD0']?.['Artist'] || exifData['DB_Artist'] || '';
    form.copyright = exifData['Exif IFD0']?.['Copyright'] || exifData['DB_Copyright'] || '';
    form.kameraHersteller = exifData['Exif IFD0']?.['Make'] || exifData['DB_CameraMake'] || '';
    form.kameraModell = exifData['Exif IFD0']?.['Model'] || exifData['DB_CameraModel'] || '';

    form.objektivHersteller = exifData['Exif SubIFD']?.['Lens Make'] || exifData['DB_LensMake'] || '';
    form.objektivModell = exifData['Exif SubIFD']?.['Lens Model'] || exifData['DB_LensModel'] || '';
    // hier später Ref zu S UND W ändern
    form.latitude = exifData?.GPS?.['GPS Latitude'] ? dmsToDecimal(exifData.GPS['GPS Latitude'], exifData.GPS['GPS Latitude Ref']) : 0;
    form.longitude = exifData?.GPS?.['GPS Longitude'] ? dmsToDecimal(exifData.GPS['GPS Longitude'], exifData.GPS['GPS Longitude Ref']) : 0;

    // Adresse aus den Backend-Zusatzdaten
    form.address = exifData['Address'] ?? '';
  });

    function toggleEdit() {
    editMode = !editMode;
  }

  async function updateExif() {

    const payload = { ...form };

    try {
      const res = await fetch(
        `http://localhost:8080/testapp-1.0/api/foto/update-exif/${filename}`,
        {
          method: 'POST', // bleibt POST, REST-Seite muss darauf reagieren
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        }
      );

      if (!res.ok) throw new Error('Aktualisieren fehlgeschlagen');

      const updatedData = await res.json();
      exifData = updatedData; // Metadaten-Tabelle aktualisieren
      location.reload();
    } catch (err) {
      alert((err as Error).message);
    }
  }
// ######################################################################################

  onMount(async () => {
    const state = history.state;
    const filename = new URLSearchParams(window.location.search).get('image');

    if (state?.fileUrl && state?.exifData) {
      // Zustand von goto vorhanden
      fileUrl = state.fileUrl;
      exifData = state.exifData;
    } else if (filename) {
      // Direktaufruf → Daten vom Backend laden
      fileUrl = `http://localhost:8080/testapp-1.0/api/foto/image/${filename}`;
      try {
        const res = await fetch(`http://localhost:8080/testapp-1.0/api/foto/update-exif-by-name/${filename}`);
        if (!res.ok) throw new Error('Fehler beim Laden der Metadaten');
        exifData = await res.json();
        console.log('Raw Exif', exifData);
      } catch (err) {
        alert((err as Error).message);
      }
    } else {
      // Kein Bild → zurück zur Galerie
      goto('/');
    }
  });

  function goBack() {
    goto('/');
  }
// Rechnet in Decimal um und beachtet jetzt auch die Ref der GPS Werte.
  function dmsToDecimal(dmsStr: string, ref: string): number {
  if (!dmsStr) return 0;

  const match = dmsStr.match(/(\d+)° (\d+)' ([\d,\.]+)"/);
  if (!match) return 0;

  const deg = parseFloat(match[1]);
  const min = parseFloat(match[2]);
  const sec = parseFloat(match[3].replace(',', '.'));
  
  let dec = deg + min / 60 + sec / 3600;

  if (ref != null && (ref.toUpperCase() === 'S' || ref.toUpperCase() === 'W')) {
    dec = -dec;
  }

  return dec;
}

  let geocodeQuery = '';

  async function performGeocode() {
    if (!geocodeQuery) return;

    try {
      const res = await fetch(`http://localhost:8080/testapp-1.0/api/foto/geocode?q=${encodeURIComponent(geocodeQuery)}`);
      if (!res.ok) throw new Error('Geocoding fehlgeschlagen oder keine Ergebnisse');
      
      const data = await res.json();
      if (Array.isArray(data) && data.length > 0) {
        const result = data[0];
        form.latitude = parseFloat(result.lat);
        form.longitude = parseFloat(result.lon);
        form.address = result.display_name;
        
        // Speichere das komplette erste Ergebnis-Objekt als String für die Datenbank
        form.fotoDateiGeo = JSON.stringify(result);
        
        alert('Koordinaten erfolgreich aktualisiert!');
      } else {
        alert('Keine Ergebnisse gefunden.');
      }
    } catch (err) {
      alert((err as Error).message);
    }
  }

</script>


<h2>Bild Details</h2>
<button class="btn back-btn" on:click={() => goto('/')}>Zurück</button>
<button class="btn edit-btn" on:click={toggleEdit}>
  {editMode ? 'Abbrechen' : 'Bearbeiten'}
</button>

{#if fileUrl}
  <div class="detail-container">
    <img src={fileUrl} alt="Bild" class="detail-image" />

    {#if editMode}
      <h3>Metadaten bearbeiten</h3>
      <div class="form-grid">
        <label>
          Page Name:
          <input type="text" bind:value={form.name} />
        </label>
        <label>
          Titel:
          <input type="text" bind:value={form.titel} />
        </label>
        <label>
          Betreff:
          <input type="text" bind:value={form.betreff} />
        </label>
        <label>
          Artist:
          <input type="text" bind:value={form.autoren} />
        </label>
        <label>
          Copyright:
          <input type="text" bind:value={form.copyright} />
        </label>
        <label>
          Kamera-Hersteller:
          <input type="text" bind:value={form.kameraHersteller} />
        </label>
        <label>
          Kamera-Modell:
          <input type="text" bind:value={form.kameraModell} />
        </label>
        <label>
          Objektiv-Hersteller:
          <input type="text" bind:value={form.objektivHersteller} />
        </label>
        <label>
          Objektiv-Modell:
          <input type="text" bind:value={form.objektivModell} />
        </label>
        <label>
  Latitude:
  <input type="number" step="any" bind:value={form.latitude} readonly />
</label>
<label>
  Longitude:
  <input type="number" step="any" bind:value={form.longitude} readonly />
</label>
<label>
  Adresse:
  <input type="text" bind:value={form.address} readonly />
</label>

<label style="grid-column: 1 / -1; display: flex; flex-direction: row; gap: 0.5rem; align-items: flex-end;">
  <div style="flex-grow: 1;">
    Geocoding Suche:
    <input type="text" bind:value={geocodeQuery} placeholder="Adresse eingeben..." style="width: 100%;" />
  </div>
  <button type="button" class="btn edit-btn" on:click={performGeocode} style="margin: 0; padding: 0.5rem;">Suchen</button>
</label>
      </div>
      <button class="btn update-btn" on:click={updateExif}>Aktualisieren</button>
    {/if}

    <h3>EXIF / XMP Daten</h3>
    {#if Object.keys(exifData).length === 0}
      <p>Keine EXIF/XMP-Daten gefunden.</p>
    {:else}
      {#each Object.entries(exifData) as [directory, tags]}
        <h4>{directory}</h4>
        <table>
          <thead>
            <tr><th>Feld</th><th>Wert</th></tr>
          </thead>
          <tbody>
            {#each Object.entries(tags) as [tagName, tagValue]}
              <tr>
                <td>{tagName}</td>
                <td>{tagValue}</td>
              </tr>
            {/each}
          </tbody>
        </table>
      {/each}
    {/if}
  </div>
{/if}

<style>
.detail-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  margin-top: 1rem;
}

.detail-image {
  max-width: 90vw;
  max-height: 70vh;
  object-fit: contain;
  border-radius: 10px;
  box-shadow: 0 0 10px rgba(0,0,0,0.2);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  width: 90%;
  margin-top: 1rem;
}

label {
  display: flex;
  flex-direction: column;
  font-weight: 500;
  margin: 0
}

input {
  padding: 0.25rem 0.5rem;
  border: 1px solid #ccc;
  border-radius: 5px;
  margin: 0
}

button {
  margin-top: 0.5rem;
  padding: 0.5rem 1rem;
  cursor: pointer;
  border-radius: 5px;
}

.btn.back-btn {
  background: #4caf50;
  color: white;
  border: none;
}

.btn.edit-btn {
  background: #2196f3;
  color: white;
  border: none;
  margin-left: 0.5rem;
}

.btn.update-btn {
  background: #ff9800;
  color: white;
  border: none;
}
</style>

