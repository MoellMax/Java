<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { afterUpdate } from 'svelte';
  import { page } from '$app/stores';

  let serverImages: any[] = [];
  let loadingServer = true;
  let uploading = false;
  let filterMode = 'all';
  let searchQuery = "";
  let isSearchActive = false;
  let searchTimer: ReturnType<typeof setTimeout>;
  
  // Pagination-State
  let currentPage = 1;
  let pageSize = 32;
  let hasMore = true;

  // Galerie-Daten vom Backend laden
  async function loadServerImages(page = 1) {
    loadingServer = true;
    currentPage = page;
    try {
      let url = 'http://localhost:8080/testapp-1.0/api/foto/images';
      const params = new URLSearchParams();
      
      params.append('page', currentPage.toString());
      params.append('pageSize', pageSize.toString());

      if (filterMode !== 'all') {
        params.append('filter', filterMode);
      }

      if (searchQuery.trim() !== "") {
        url = 'http://localhost:8080/testapp-1.0/api/foto/search';
        params.append('q', searchQuery);
        isSearchActive = true;
      } else {
         isSearchActive = false;
      }
      
      const queryString = params.toString();
      if (queryString) url += `?${queryString}`;

      console.log("Fetching: " + url);

      const res = await fetch(url);
      if (!res.ok) throw new Error('Fehler beim Laden der Bilder');
      const data = await res.json();
      serverImages = data;
      
      // Einfache Prüfung, ob es mehr geben könnte
      hasMore = data.length === pageSize;
    } catch (err) {
      alert((err as Error).message);
    } finally {
      loadingServer = false;
    }
  }

  function changePage(delta: number) {
    const newPage = currentPage + delta;
    if (newPage >= 1 && (delta < 0 || hasMore)) {
      loadServerImages(newPage);
    }
  }

  function resetAndLoad() {
    loadServerImages(1);
  }

  function searchImages() {
    loadServerImages();
  }

  function handleSearchInput() {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
      resetAndLoad();
    }, 400); // 400ms debounce
  }



  async function downloadImage(filename: string) {
  try {
    const res = await fetch(`http://localhost:8080/testapp-1.0/api/foto/image/${filename}`);
    if (!res.ok) throw new Error('Download fehlgeschlagen');

    const blob = await res.blob();
    const url = URL.createObjectURL(blob);

    // Datei-Endung erhalten
    const dotIndex = filename.lastIndexOf('.');
    const newFilename =
      dotIndex !== -1
        ? filename.substring(0, dotIndex) + '-Kopie' + filename.substring(dotIndex)
        : filename + '-Kopie';

    const link = document.createElement('a');
    link.href = url;
    link.download = newFilename; // angepasster Name
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    URL.revokeObjectURL(url);
  } catch (err) {
    alert((err as Error).message);
  }
  }

onMount(loadServerImages);



  // Upload-Funktion - PARALLEL UPLOADS for speed!
  async function uploadFiles(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    uploading = true;
    try {
      // Create array of upload promises for PARALLEL execution
      const uploadPromises = Array.from(input.files)
        .filter(file => ['image/jpeg','image/png'].includes(file.type))
        .map(file => {
          const formData = new FormData();
          formData.append('file', file);
          
          return fetch('http://localhost:8080/testapp-1.0/api/foto/upload', {
            method: 'POST',
            body: formData
          }).then(res => {
            if (!res.ok) throw new Error(`Upload fehlgeschlagen: ${file.name}`);
            return res;
          });
        });

      // Wait for ALL uploads to complete in parallel
      await Promise.all(uploadPromises);

      await loadServerImages(1); // Galerie nach Upload auf Seite 1 aktualisieren
    } catch (err) {
      alert((err as Error).message);
    } finally {
      uploading = false;
      (event.target as HTMLInputElement).value = '';
    }
  }

  // EXIF/XMP Button
  async function loadExif(filename: string) {
    try {
      const res = await fetch(`http://localhost:8080/testapp-1.0/api/foto/update-exif-by-name/${filename}`);
      if (!res.ok) throw new Error('Fehler beim Laden der Metadaten');

      const exifData = await res.json();
      goto(`/bildDetails?image=${filename}`, {
        state: {
          fileUrl: `http://localhost:8080/testapp-1.0/api/foto/image/${filename}`,
          exifData
        }
      });
    } catch (err) {
      alert((err as Error).message);
    }
  }



  // Löschen Button
async function deleteImage(uniqueName: string, displayName: string) {
  if (!confirm(`Bild "${displayName}" wirklich löschen?`)) return;
  try {
    const res = await fetch(`http://localhost:8080/testapp-1.0/api/foto/delete/${uniqueName}`, {
      method: 'DELETE'
    });
    if (!res.ok) throw new Error('Löschen fehlgeschlagen');
    await loadServerImages(currentPage); // Aktuelle Seite neu laden
  } catch (err) {
    alert((err as Error).message);
  }
}

  // Alle Bilder löschen Funktion
  async function deleteAllImages() {
    if (!confirm('Wirklich ALLE Bilder und deren Metadaten löschen?')) return;
    try {
      const res = await fetch('http://localhost:8080/testapp-1.0/api/foto/delete-all', {
        method: 'DELETE'
      });
      if (!res.ok) throw new Error('Löschen aller Bilder fehlgeschlagen');
      await loadServerImages(1);
    } catch (err) {
      alert((err as Error).message);
    }
  }


</script>

<h2>Bilder hochladen</h2>
<input type="file" accept="image/png, image/jpeg, image/jpg" multiple on:change={uploadFiles} disabled={uploading} />
{#if uploading}<p>Upload läuft…</p>{/if}

<button class="delete-all-btn" on:click={deleteAllImages} disabled={uploading}>
  Alle Bilder löschen
</button>

<div class="filter-controls">
  <label><input type="radio" bind:group={filterMode} value="no_gps" on:change={resetAndLoad}> Ohne GPS</label>
  <label><input type="radio" bind:group={filterMode} value="gps" on:change={resetAndLoad}> Mit GPS</label>
  <label><input type="radio" bind:group={filterMode} value="all" on:change={resetAndLoad}> Alle</label>
</div>
<div class="search-bar">
  <input type="text" bind:value={searchQuery} placeholder="Nach Metadaten suchen..." on:input={handleSearchInput} on:keydown={(e) => e.key === 'Enter' && resetAndLoad()} />
</div>

<div class="pagination-controls">
  <button on:click={() => changePage(-1)} disabled={currentPage === 1 || loadingServer}>Zurück</button>
  <span>Seite {currentPage}</span>
  <button on:click={() => changePage(1)} disabled={!hasMore || loadingServer}>Weiter</button>
</div>

{#if loadingServer}<p>Lade Bilder vom Server…</p>
{:else if serverImages.length === 0 && isSearchActive}<p>Keine Ergebnisse gefunden.</p>
{:else if serverImages.length === 0}<p>Keine Bilder vorhanden.</p>
{:else}
<div class="gallery-grid">
  {#each serverImages as image}
    <div class="gallery-item">
      <img src={`http://localhost:8080/testapp-1.0/api/foto/image/${image.uniqueName}`} alt={image.displayName} loading="lazy" />
      <p>{image.displayName}</p>
      <button on:click={() => loadExif(image.uniqueName)}>Metadaten anzeigen</button>
      <!-- Download Button -->
      <button class="btn download-btn" on:click={() => downloadImage(image.uniqueName)}>
       Download
      </button>
      <button on:click={() => deleteImage(image.uniqueName, image.displayName)}>Löschen</button>
    </div>
  {/each}
</div>
{/if}

<style>
/* === Layout-Grundlage === */
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 1.25rem;
}
 
/* === Galerie-Karte === */
.gallery-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.75rem;
  border-radius: 10px;
  background: #353b4985; /* Schwarz */
  box-shadow: 0 2px 6px rgba(0,0,0,0.2);
  color: #fff; /* Weißer Text */
}
 
/* === Bild === */
.gallery-item img {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border-radius: 8px;
}
 
/* === Dateiname === */
.gallery-item p {
  margin: 0.5rem 0 0.25rem;
  text-align: center;
  font-size: 0.9rem;
  color: #fff; /* Weißer Text */
  word-break: break-word;
}
 
/* === Buttons (außer Alle Löschen) === */
.gallery-item button {
  width: 100%;
  margin-top: 0.3rem;
  padding: 0.4rem;
  font-size: 0.8rem;
  border-radius: 6px;
  border: none;
  background: hsl(209, 63%, 26%); /* Schwarz */
  color: #fff; /* Weißer Text */
  cursor: pointer;
}
 
.gallery-item button:hover {
  background: #222; /* leichtes Hover-Dunkel */
}
 
/* === Filter & Suche === */
.filter-controls {
  display: flex;
  flex-direction: row;  /* nebeneinander */
  align-items: center;  /* vertikal zentriert */
  gap: 1.5rem;          /* Abstand zwischen den Optionen */
  margin-bottom: 0.75rem;
}
 
.filter-controls label {
  display: flex;
  align-items: center;  /* Text und Radio perfekt mittig */
  gap: 0.3rem;
  line-height: 1;
  color: #fff;          /* Weißer Text */
  margin: 0;            /* verhindert unterschiedliche Abstände */
}
 
.search-bar {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
 
.search-bar input {
  flex: 1;
  padding: 0.45rem;
  border-radius: 6px;
  border: 1px solid #444;
  background: #353b4985;
  color: #fff;
}
 
/* === Pagination === */
.pagination-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}
 
.pagination-controls span {
  font-weight: 500;
  color: #fff; /* Weißer Text */
}
 
/* === Delete All === */
.delete-all-btn {
  background: #ff5c5c; /* Rot bleibt */
  color: white;
  border: none;
  padding: 1rem 1rem;
  border-radius: 18px;
  margin-bottom: 1.5rem;
}
 
.delete-all-btn:hover {
  background: #e04848;
}
 
 
</style>
