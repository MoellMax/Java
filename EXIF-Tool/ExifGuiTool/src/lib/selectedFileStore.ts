import { writable } from 'svelte/store';

export const selectedFileStore = writable<{ fileUrl: string; exifData: any } | null>(null);
 
