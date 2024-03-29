package net.nnwsf.persistence;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="test")
public class TestEntity {
    
    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="name")
    private String name;

    @OneToMany(targetEntity=TestEntity2.class, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<TestEntity2> test2List;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TestEntity2> getTest2List() {
        return test2List;
    }

    public void setTest2List(List<TestEntity2> test2List) {
        this.test2List = test2List;
    }
}