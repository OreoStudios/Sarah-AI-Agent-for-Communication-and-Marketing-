import { useCallback, useEffect, useState } from 'react';
import { api } from '../api';
export function useResource<T>(url:string) {
 const [data,setData]=useState<T|null>(null); const [error,setError]=useState<string|null>(null); const [loading,setLoading]=useState(true);
 const reload=useCallback(async()=>{setLoading(true);setError(null);try{const response=await api.get<T>(url);setData(response.data);}catch{setError('We couldn’t load this information. Check your connection and try again.');}finally{setLoading(false);}},[url]);
 useEffect(()=>{void reload();},[reload]); return {data,error,loading,reload};
}
