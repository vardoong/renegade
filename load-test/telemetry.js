import http from 'k6/http';
import { check } from 'k6';
import { scenario } from 'k6/execution';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
    summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
    scenarios: {
        telemetry: {
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.RATE || '50'),
            timeUnit: '1s',
            duration: __ENV.DURATION || '3m',
            preAllocatedVUs: 100,
        },
    },
};

export default function () {
    const payload = {
        vehicleId: 11 + (scenario.iterationInTest % 50),
        speed: 40,
        rpm: 1600,
        batteryLevel: 80,
        temperature: 25,
        latitude: 37.5012,
        longitude: 127.0396,
        timestamp: new Date().toISOString(),
    };

    const response = http.post(
        `${baseUrl}/api/v1/telemetry`,
        JSON.stringify(payload),
        { headers: { 'Content-Type': 'application/json' } },
    );

    check(response, { 'telemetry accepted': (res) => res.status >= 200 && res.status < 300 });
}