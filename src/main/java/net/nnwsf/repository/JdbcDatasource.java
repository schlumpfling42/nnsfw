package net.nnwsf.repository;

public @interface JdbcDatasource {
    String url();
    String username();
    String password();
}
