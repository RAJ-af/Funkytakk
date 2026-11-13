-- create_moment: store the language name only ("Marathi (Beginner)" -> "Marathi"),
-- otherwise the My language tab (which compares plain names) never matches such posts.
do $do$ declare d text;
begin
  d := pg_get_functiondef('public.create_moment(text,text,text,text,text,jsonb,text[],text,boolean)'::regprocedure);
  if position('regexp_replace(coalesce(p_language' in d) = 0 then
    d := replace(d,
      'v_lang text := nullif(btrim(coalesce(p_language,'''')), '''');',
      'v_lang text := nullif(btrim(regexp_replace(coalesce(p_language,''''), ''\s*\([^)]*\)\s*$'', '''')), '''');');
    execute d;
  end if;
end $do$;

-- repair rows already saved with a level suffix
update public.moments
   set language = btrim(regexp_replace(language, '\s*\([^)]*\)\s*$', ''))
 where language ~ '\([^)]*\)\s*$';
