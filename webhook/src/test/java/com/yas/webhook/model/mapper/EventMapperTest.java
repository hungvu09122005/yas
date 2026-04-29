package com.yas.webhook.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.webhook.model.Event;
import com.yas.webhook.model.viewmodel.webhook.EventVm;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class EventMapperTest {

    private final EventMapper eventMapper = Mappers.getMapper(EventMapper.class);

    @Test
    void toEventVm_ShouldMapCorrectly() {
        Event event = new Event();
        event.setId(1L);
        event.setName("PRODUCT_CREATED");
        event.setDescription("Product created event");

        EventVm eventVm = eventMapper.toEventVm(event);

        assertThat(eventVm).isNotNull();
        assertThat(eventVm.id()).isEqualTo(1L);
        assertThat(eventVm.name()).isEqualTo("PRODUCT_CREATED");
        assertThat(eventVm.description()).isEqualTo("Product created event");
    }

    @Test
    void toEventVm_WhenNull_ShouldReturnNull() {
        EventVm eventVm = eventMapper.toEventVm(null);
        assertThat(eventVm).isNull();
    }
}
