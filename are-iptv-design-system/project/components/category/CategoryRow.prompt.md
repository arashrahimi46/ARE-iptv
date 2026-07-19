**CategoryRow** — compact category list row for the filter column down the left of Live TV, Movies, Series and the EPG guide. Kind icon + name + count + chevron.

```jsx
<CategoryRow kind="live" name="All channels" count={1240} active />
<CategoryRow kind="live" name="Sports" count={128} />
<CategoryRow kind="movies" name="Recently added" count={90} smart />
```
`active` = accent wash + edge bar; focus (D-pad) lifts the surface with the accent ring. Stack in a scrollable column.
