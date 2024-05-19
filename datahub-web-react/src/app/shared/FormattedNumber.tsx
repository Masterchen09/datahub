import React from 'react';

import { Tooltip } from 'antd';

import { formatNumber, formatNumberWithoutAbbreviation } from './formatNumber';

interface FormattedNumberProps {
    value: any;
}

export default function FormattedNumber({ value }: FormattedNumberProps) {
    const formattedValue = formatNumber(value);

    if (formattedValue !== value) {
        return (
            <Tooltip title={formatNumberWithoutAbbreviation(value)}>
                {formattedValue}
            </Tooltip>
        )
    }

    return value;
}