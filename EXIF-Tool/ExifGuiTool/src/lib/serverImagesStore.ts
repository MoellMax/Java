import { writable } from 'svelte/store';

export const serverImagesStore = writable<string[]>([]); 
