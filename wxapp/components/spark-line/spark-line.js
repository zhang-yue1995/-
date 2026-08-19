Component({
  properties: {
    data: {
      type: Array,
      value: []
    },
    color: {
      type: String,
      value: '#0e8f78'
    },
    width: {
      type: Number,
      value: 200
    },
    height: {
      type: Number,
      value: 60
    }
  },

  data: {
    id: Math.random().toString(36).substr(2, 9)
  },

  lifetimes: {
    attached() {
      this.drawChart();
    }
  },

  observers: {
    'data, color': function() {
      this.drawChart();
    }
  },

  methods: {
    drawChart() {
      const { data, color, width, height } = this.properties;
      const ctx = wx.createCanvasContext(`sparkline-${this.data.id}`, this);

      const values = (data || []).map(Number).filter(Number.isFinite);
      ctx.clearRect(0, 0, width, height);
      if (values.length === 0) {
        ctx.draw();
        return;
      }

      const min = Math.min(...values);
      const max = Math.max(...values);
      const range = max - min || 1;

      ctx.setStrokeStyle(color);
      ctx.setLineWidth(2);
      ctx.setLineCap('round');
      ctx.setLineJoin('round');

      values.forEach((value, index) => {
        const x = values.length === 1 ? width / 2 : (index / (values.length - 1)) * width;
        const y = height - ((value - min) / range) * (height - 10) - 5;

        if (index === 0) {
          ctx.moveTo(x, y);
        } else {
          ctx.lineTo(x, y);
        }
      });

      ctx.stroke();

      // 绘制最后一个点
      if (values.length > 0) {
        const lastX = values.length === 1 ? width / 2 : width;
        const lastY = height - ((values[values.length - 1] - min) / range) * (height - 10) - 5;
        ctx.setFillStyle(color);
        ctx.beginPath();
        ctx.arc(lastX, lastY, 3, 0, 2 * Math.PI);
        ctx.fill();
      }

      ctx.draw();
    }
  }
});
