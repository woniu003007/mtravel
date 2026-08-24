import { describe, expect, it } from 'vitest';

type ArrangementTarget =
  | { allowRepeat?: boolean; role: 'accommodation' | 'ground_service' | 'itinerary' }
  | { requiresMealSelection: true }
  | { unsupportedInDayMap: true };

async function loadArrangementUtils(): Promise<{
  resolveArrangementTarget?: (resourceType: string) => ArrangementTarget;
}> {
  const modulePath = './components/product-designer-arrangement-utils';
  return import(/* @vite-ignore */ modulePath).catch(() => ({}));
}

describe('product designer resource arrangement target', () => {
  it('routes every map resource type to its explicit day target', async () => {
    const { resolveArrangementTarget } = await loadArrangementUtils();

    expect(resolveArrangementTarget?.('hotel')).toEqual({ role: 'accommodation' });
    expect(resolveArrangementTarget?.('restaurant')).toEqual({ requiresMealSelection: true });
    expect(resolveArrangementTarget?.('scenic')).toEqual({ role: 'itinerary' });
    expect(resolveArrangementTarget?.('shopping')).toEqual({ role: 'itinerary' });
    expect(resolveArrangementTarget?.('other')).toEqual({ role: 'itinerary' });
    expect(resolveArrangementTarget?.('ground_agent')).toEqual({ unsupportedInDayMap: true });
    expect(resolveArrangementTarget?.('vehicle')).toEqual({ unsupportedInDayMap: true });
    expect(resolveArrangementTarget?.('traffic')).toEqual({ unsupportedInDayMap: true });
  });
});
