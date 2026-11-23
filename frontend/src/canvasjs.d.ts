declare module '@canvasjs/charts' {
  namespace CanvasJS {
    interface ChartDataPoint {
      x?: number | Date;
      y?: number;
      label?: string;
      [key: string]: unknown;
    }

    interface ChartDataSeriesOptions {
      type?: string;
      color?: string;
      lineThickness?: number;
      markerType?: string;
      markerSize?: number;
      cornerRadiusY?: number;
      dataPoints?: ChartDataPoint[];
      [key: string]: unknown;
    }

    interface ChartAxisOptions {
      title?: string;
      labelFontSize?: number;
      labelFontColor?: string;
      labelFormatter?: (e: { value: Date | number }) => string;
      gridThickness?: number;
      gridColor?: string;
      lineThickness?: number;
      tickLength?: number;
      minimum?: number;
      interval?: number;
      intervalType?: string;
      [key: string]: unknown;
    }

    interface ChartOptions {
      animationEnabled?: boolean;
      backgroundColor?: string;
      title?: { text?: string; [key: string]: unknown };
      axisX?: ChartAxisOptions;
      axisY?: ChartAxisOptions;
      toolTip?: { borderThickness?: number; cornerRadius?: number; content?: string; [key: string]: unknown };
      data?: ChartDataSeriesOptions[];
      [key: string]: unknown;
    }

    class Chart {
      constructor(container: HTMLElement, options: ChartOptions);
      options: ChartOptions;
      render(): void;
      destroy(): void;
    }
  }

  const CanvasJS: typeof CanvasJS;
  export = CanvasJS;
}
