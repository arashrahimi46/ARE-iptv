**Dialog** — modal sheet on a blurred scrim. Confirmations, parental PIN, add-source. Render inside a `position:relative` container.

```jsx
<Dialog title="Remove this playlist?" onClose={close}
  actions={<><Button variant="ghost">Cancel</Button><Button variant="danger">Remove</Button></>}>
  Living Room Portal and its 842 channels will be removed from this device.
</Dialog>
```
