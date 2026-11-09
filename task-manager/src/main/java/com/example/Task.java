// extending PanacheEntity, quarkus automatically provide auto-generated primary key (public Long id)
// and built in operations like Task.persist() and Task.listAll()

// @Entity annotation automatically generates corresponding Task table inside postgres container


package com.example;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class Task extends PanacheEntity {
    public String title;
    public boolean completed;
}
