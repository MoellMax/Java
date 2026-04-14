import { writable } from 'svelte/store';

export interface FileItem {
  file: File;
  id: string; // eindeutige ID für jedes Bild
}

export const uploadedFiles = writable<FileItem[]>([]);
 
