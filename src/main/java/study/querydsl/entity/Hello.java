package study.querydsl.entity;

import lombok.Generated;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Getter
@Setter
public class Hello {

    @Id
    @Generated
    private Long id;
}
