Component({
  properties: {
    type: {
      type: String,
      value: 'ocr'
    },
    title: {
      type: String,
      value: ''
    },
    desc: {
      type: String,
      value: ''
    },
    icon: {
      type: String,
      value: '📷'
    }
  },
  data: {},
  methods: {
    onTap() {
      this.triggerEvent('tap', { type: this.properties.type });
    }
  }
});